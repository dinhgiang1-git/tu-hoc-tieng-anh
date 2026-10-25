-- =============================================================================
-- Migration: V004__enforce_published_revision_immutability.sql
-- Description: Enforces deep immutability on PUBLISHED and ARCHIVED curriculum
--              revisions, child entities (phases, weeks, lessons, activities,
--              resources), and activity-resource links.
--              Guards both OLD and NEW relations on UPDATE to prevent stealing/
--              reassigning child entities between published and draft revisions.
--              Guards published_at and created_at with null-safe IS DISTINCT FROM.
-- Conventions: BE_CODE_CONVENTION.md, ADR-005
-- =============================================================================

-- 1. Helper function: verify revision immutability by ID
CREATE OR REPLACE FUNCTION check_revision_immutable_by_id(rev_id UUID, action_desc TEXT)
RETURNS VOID AS $$
DECLARE
    rev_status VARCHAR(20);
BEGIN
    IF rev_id IS NOT NULL THEN
        SELECT status INTO rev_status FROM curriculum_revision WHERE id = rev_id;
        IF rev_status IN ('PUBLISHED', 'ARCHIVED') THEN
            RAISE EXCEPTION 'Cannot % a % curriculum revision', action_desc, rev_status;
        END IF;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- 2. Helper functions to traverse foreign keys up to revision_id
CREATE OR REPLACE FUNCTION get_revision_id_by_phase(p_phase_id UUID)
RETURNS UUID AS $$
DECLARE
    r_id UUID;
BEGIN
    IF p_phase_id IS NULL THEN
        RETURN NULL;
    END IF;
    SELECT revision_id INTO r_id FROM curriculum_phase WHERE id = p_phase_id;
    RETURN r_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_revision_id_by_week(p_week_id UUID)
RETURNS UUID AS $$
DECLARE
    r_id UUID;
BEGIN
    IF p_week_id IS NULL THEN
        RETURN NULL;
    END IF;
    SELECT p.revision_id INTO r_id
    FROM curriculum_week w
    JOIN curriculum_phase p ON p.id = w.phase_id
    WHERE w.id = p_week_id;
    RETURN r_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_revision_id_by_lesson(p_lesson_id UUID)
RETURNS UUID AS $$
DECLARE
    r_id UUID;
BEGIN
    IF p_lesson_id IS NULL THEN
        RETURN NULL;
    END IF;
    SELECT p.revision_id INTO r_id
    FROM lesson l
    JOIN curriculum_week w ON w.id = l.week_id
    JOIN curriculum_phase p ON p.id = w.phase_id
    WHERE l.id = p_lesson_id;
    RETURN r_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_revision_id_by_activity(p_activity_id UUID)
RETURNS UUID AS $$
DECLARE
    r_id UUID;
BEGIN
    IF p_activity_id IS NULL THEN
        RETURN NULL;
    END IF;
    SELECT p.revision_id INTO r_id
    FROM activity a
    JOIN lesson l ON l.id = a.lesson_id
    JOIN curriculum_week w ON w.id = l.week_id
    JOIN curriculum_phase p ON p.id = w.phase_id
    WHERE a.id = p_activity_id;
    RETURN r_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_revision_id_by_resource(p_resource_id UUID)
RETURNS UUID AS $$
DECLARE
    r_id UUID;
BEGIN
    IF p_resource_id IS NULL THEN
        RETURN NULL;
    END IF;
    SELECT revision_id INTO r_id FROM resource WHERE id = p_resource_id;
    RETURN r_id;
END;
$$ LANGUAGE plpgsql;

-- 3. Revision Immutability: prevent reverting to DRAFT, protect all metadata & timestamps
CREATE OR REPLACE FUNCTION trg_check_revision_immutability()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IN ('PUBLISHED', 'ARCHIVED') THEN
        -- Prevent reverting to DRAFT
        IF NEW.status = 'DRAFT' THEN
            RAISE EXCEPTION 'Cannot revert % curriculum revision to DRAFT', OLD.status;
        END IF;

        -- Prevent reviving ARCHIVED revisions
        IF OLD.status = 'ARCHIVED' AND NEW.status <> 'ARCHIVED' THEN
            RAISE EXCEPTION 'ARCHIVED curriculum revision cannot change status';
        END IF;

        -- Metadata, published_at, and created_at are strictly immutable across any update
        -- (including when transitioning from PUBLISHED to ARCHIVED)
        IF OLD.title IS DISTINCT FROM NEW.title
           OR OLD.description IS DISTINCT FROM NEW.description
           OR OLD.authors IS DISTINCT FROM NEW.authors
           OR OLD.provenance IS DISTINCT FROM NEW.provenance
           OR OLD.checksum IS DISTINCT FROM NEW.checksum
           OR OLD.version IS DISTINCT FROM NEW.version
           OR OLD.curriculum_id IS DISTINCT FROM NEW.curriculum_id
           OR OLD.published_at IS DISTINCT FROM NEW.published_at
           OR OLD.created_at IS DISTINCT FROM NEW.created_at THEN
            RAISE EXCEPTION '% curriculum revision is immutable and its metadata/timestamps cannot be modified', OLD.status;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_curriculum_revision_immutable ON curriculum_revision;
CREATE TRIGGER trg_curriculum_revision_immutable
BEFORE UPDATE ON curriculum_revision
FOR EACH ROW
EXECUTE FUNCTION trg_check_revision_immutability();

-- 4. Revision Delete Protection
CREATE OR REPLACE FUNCTION trg_check_revision_delete()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IN ('PUBLISHED', 'ARCHIVED') THEN
        RAISE EXCEPTION 'Cannot delete a % curriculum revision', OLD.status;
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_curriculum_revision_prevent_delete ON curriculum_revision;
CREATE TRIGGER trg_curriculum_revision_prevent_delete
BEFORE DELETE ON curriculum_revision
FOR EACH ROW
EXECUTE FUNCTION trg_check_revision_delete();

-- 5. Phase Immutability (guards both OLD and NEW on UPDATE)
CREATE OR REPLACE FUNCTION trg_check_phase_immutable()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        PERFORM check_revision_immutable_by_id(OLD.revision_id, 'modify or delete phase of');
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        PERFORM check_revision_immutable_by_id(NEW.revision_id, 'insert or reassign phase to');
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_phase_immutable ON curriculum_phase;
CREATE TRIGGER trg_phase_immutable
BEFORE INSERT OR UPDATE OR DELETE ON curriculum_phase
FOR EACH ROW
EXECUTE FUNCTION trg_check_phase_immutable();

-- 6. Week Immutability (guards both OLD and NEW on UPDATE)
CREATE OR REPLACE FUNCTION trg_check_week_immutable()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        PERFORM check_revision_immutable_by_id(get_revision_id_by_phase(OLD.phase_id), 'modify or delete week of');
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        PERFORM check_revision_immutable_by_id(get_revision_id_by_phase(NEW.phase_id), 'insert or reassign week to');
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_week_immutable ON curriculum_week;
CREATE TRIGGER trg_week_immutable
BEFORE INSERT OR UPDATE OR DELETE ON curriculum_week
FOR EACH ROW
EXECUTE FUNCTION trg_check_week_immutable();

-- 7. Lesson Immutability (guards both OLD and NEW on UPDATE)
CREATE OR REPLACE FUNCTION trg_check_lesson_immutable()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        PERFORM check_revision_immutable_by_id(get_revision_id_by_week(OLD.week_id), 'modify or delete lesson of');
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        PERFORM check_revision_immutable_by_id(get_revision_id_by_week(NEW.week_id), 'insert or reassign lesson to');
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_lesson_immutable ON lesson;
CREATE TRIGGER trg_lesson_immutable
BEFORE INSERT OR UPDATE OR DELETE ON lesson
FOR EACH ROW
EXECUTE FUNCTION trg_check_lesson_immutable();

-- 8. Activity Immutability (guards both OLD and NEW on UPDATE)
CREATE OR REPLACE FUNCTION trg_check_activity_immutable()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        PERFORM check_revision_immutable_by_id(get_revision_id_by_lesson(OLD.lesson_id), 'modify or delete activity of');
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        PERFORM check_revision_immutable_by_id(get_revision_id_by_lesson(NEW.lesson_id), 'insert or reassign activity to');
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_activity_immutable ON activity;
CREATE TRIGGER trg_activity_immutable
BEFORE INSERT OR UPDATE OR DELETE ON activity
FOR EACH ROW
EXECUTE FUNCTION trg_check_activity_immutable();

-- 9. Resource Immutability (guards both OLD and NEW on UPDATE)
CREATE OR REPLACE FUNCTION trg_check_resource_immutable()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        PERFORM check_revision_immutable_by_id(OLD.revision_id, 'modify or delete resource of');
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        PERFORM check_revision_immutable_by_id(NEW.revision_id, 'insert or reassign resource to');
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_resource_immutable ON resource;
CREATE TRIGGER trg_resource_immutable
BEFORE INSERT OR UPDATE OR DELETE ON resource
FOR EACH ROW
EXECUTE FUNCTION trg_check_resource_immutable();

-- 10. Activity-Resource Link Immutability (guards both OLD and NEW on UPDATE)
CREATE OR REPLACE FUNCTION trg_check_activity_resource_immutable()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        PERFORM check_revision_immutable_by_id(get_revision_id_by_activity(OLD.activity_id), 'modify or delete activity-resource link of');
        PERFORM check_revision_immutable_by_id(get_revision_id_by_resource(OLD.resource_id), 'modify or delete activity-resource link of');
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        PERFORM check_revision_immutable_by_id(get_revision_id_by_activity(NEW.activity_id), 'insert or reassign activity-resource link to');
        PERFORM check_revision_immutable_by_id(get_revision_id_by_resource(NEW.resource_id), 'insert or reassign activity-resource link to');
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_activity_resource_immutable ON activity_resource;
CREATE TRIGGER trg_activity_resource_immutable
BEFORE INSERT OR UPDATE OR DELETE ON activity_resource
FOR EACH ROW
EXECUTE FUNCTION trg_check_activity_resource_immutable();

-- 11. Curriculum Delete Protection when Published Revision Exists
CREATE OR REPLACE FUNCTION trg_check_curriculum_delete()
RETURNS TRIGGER AS $$
DECLARE
    published_count INT;
BEGIN
    SELECT COUNT(*) INTO published_count
    FROM curriculum_revision
    WHERE curriculum_id = OLD.id AND status IN ('PUBLISHED', 'ARCHIVED');

    IF published_count > 0 THEN
        RAISE EXCEPTION 'Cannot delete curriculum with PUBLISHED or ARCHIVED revisions';
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_curriculum_prevent_delete ON curriculum;
CREATE TRIGGER trg_curriculum_prevent_delete
BEFORE DELETE ON curriculum
FOR EACH ROW
EXECUTE FUNCTION trg_check_curriculum_delete();
