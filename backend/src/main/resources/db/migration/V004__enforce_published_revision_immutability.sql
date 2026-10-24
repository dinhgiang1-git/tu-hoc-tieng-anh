-- =============================================================================
-- Migration: V004__enforce_published_revision_immutability.sql
-- Description: Enforces deep immutability on PUBLISHED and ARCHIVED curriculum
--              revisions, child entities (phases, weeks, lessons, activities,
--              resources), and activity-resource links.
-- Conventions: BE_CODE_CONVENTION.md, ADR-005
-- =============================================================================

-- 1. Revision Immutability: prevent reverting to DRAFT, protect metadata across status changes
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

        -- Metadata is strictly immutable, even when transitioning from PUBLISHED to ARCHIVED
        IF OLD.title <> NEW.title
           OR OLD.description <> NEW.description
           OR OLD.authors <> NEW.authors
           OR OLD.provenance <> NEW.provenance
           OR OLD.checksum <> NEW.checksum
           OR OLD.version <> NEW.version
           OR OLD.curriculum_id <> NEW.curriculum_id THEN
            RAISE EXCEPTION '% curriculum revision is immutable and cannot be modified', OLD.status;
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

-- 2. Revision Delete Protection
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

-- 3. Phase Immutability
CREATE OR REPLACE FUNCTION trg_check_phase_parent_immutable()
RETURNS TRIGGER AS $$
DECLARE
    target_rev_id UUID;
    rev_status VARCHAR(20);
BEGIN
    target_rev_id := COALESCE(NEW.revision_id, OLD.revision_id);
    SELECT status INTO rev_status FROM curriculum_revision WHERE id = target_rev_id;
    IF rev_status IN ('PUBLISHED', 'ARCHIVED') THEN
        RAISE EXCEPTION 'Cannot insert, update, or delete phases of a % curriculum revision', rev_status;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_phase_immutable ON curriculum_phase;
CREATE TRIGGER trg_phase_immutable
BEFORE INSERT OR UPDATE OR DELETE ON curriculum_phase
FOR EACH ROW
EXECUTE FUNCTION trg_check_phase_parent_immutable();

-- 4. Week Immutability
CREATE OR REPLACE FUNCTION trg_check_week_parent_immutable()
RETURNS TRIGGER AS $$
DECLARE
    target_phase_id UUID;
    rev_status VARCHAR(20);
BEGIN
    target_phase_id := COALESCE(NEW.phase_id, OLD.phase_id);
    SELECT r.status INTO rev_status
    FROM curriculum_phase p
    JOIN curriculum_revision r ON r.id = p.revision_id
    WHERE p.id = target_phase_id;

    IF rev_status IN ('PUBLISHED', 'ARCHIVED') THEN
        RAISE EXCEPTION 'Cannot insert, update, or delete weeks of a % curriculum revision', rev_status;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_week_immutable ON curriculum_week;
CREATE TRIGGER trg_week_immutable
BEFORE INSERT OR UPDATE OR DELETE ON curriculum_week
FOR EACH ROW
EXECUTE FUNCTION trg_check_week_parent_immutable();

-- 5. Lesson Immutability
CREATE OR REPLACE FUNCTION trg_check_lesson_parent_immutable()
RETURNS TRIGGER AS $$
DECLARE
    target_week_id UUID;
    rev_status VARCHAR(20);
BEGIN
    target_week_id := COALESCE(NEW.week_id, OLD.week_id);
    SELECT r.status INTO rev_status
    FROM curriculum_week w
    JOIN curriculum_phase p ON p.id = w.phase_id
    JOIN curriculum_revision r ON r.id = p.revision_id
    WHERE w.id = target_week_id;

    IF rev_status IN ('PUBLISHED', 'ARCHIVED') THEN
        RAISE EXCEPTION 'Cannot insert, update, or delete lessons of a % curriculum revision', rev_status;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_lesson_immutable ON lesson;
CREATE TRIGGER trg_lesson_immutable
BEFORE INSERT OR UPDATE OR DELETE ON lesson
FOR EACH ROW
EXECUTE FUNCTION trg_check_lesson_parent_immutable();

-- 6. Activity Immutability
CREATE OR REPLACE FUNCTION trg_check_activity_parent_immutable()
RETURNS TRIGGER AS $$
DECLARE
    target_lesson_id UUID;
    rev_status VARCHAR(20);
BEGIN
    target_lesson_id := COALESCE(NEW.lesson_id, OLD.lesson_id);
    SELECT r.status INTO rev_status
    FROM lesson l
    JOIN curriculum_week w ON w.id = l.week_id
    JOIN curriculum_phase p ON p.id = w.phase_id
    JOIN curriculum_revision r ON r.id = p.revision_id
    WHERE l.id = target_lesson_id;

    IF rev_status IN ('PUBLISHED', 'ARCHIVED') THEN
        RAISE EXCEPTION 'Cannot insert, update, or delete activities of a % curriculum revision', rev_status;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_activity_immutable ON activity;
CREATE TRIGGER trg_activity_immutable
BEFORE INSERT OR UPDATE OR DELETE ON activity
FOR EACH ROW
EXECUTE FUNCTION trg_check_activity_parent_immutable();

-- 7. Resource Immutability
CREATE OR REPLACE FUNCTION trg_check_resource_parent_immutable()
RETURNS TRIGGER AS $$
DECLARE
    target_rev_id UUID;
    rev_status VARCHAR(20);
BEGIN
    target_rev_id := COALESCE(NEW.revision_id, OLD.revision_id);
    SELECT status INTO rev_status FROM curriculum_revision WHERE id = target_rev_id;
    IF rev_status IN ('PUBLISHED', 'ARCHIVED') THEN
        RAISE EXCEPTION 'Cannot insert, update, or delete resources of a % curriculum revision', rev_status;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_resource_immutable ON resource;
CREATE TRIGGER trg_resource_immutable
BEFORE INSERT OR UPDATE OR DELETE ON resource
FOR EACH ROW
EXECUTE FUNCTION trg_check_resource_parent_immutable();

-- 8. Activity-Resource Link Immutability
CREATE OR REPLACE FUNCTION trg_check_activity_resource_immutable()
RETURNS TRIGGER AS $$
DECLARE
    target_act_id UUID;
    rev_status VARCHAR(20);
BEGIN
    target_act_id := COALESCE(NEW.activity_id, OLD.activity_id);
    SELECT r.status INTO rev_status
    FROM activity a
    JOIN lesson l ON l.id = a.lesson_id
    JOIN curriculum_week w ON w.id = l.week_id
    JOIN curriculum_phase p ON p.id = w.phase_id
    JOIN curriculum_revision r ON r.id = p.revision_id
    WHERE a.id = target_act_id;

    IF rev_status IN ('PUBLISHED', 'ARCHIVED') THEN
        RAISE EXCEPTION 'Cannot insert, update, or delete activity-resource links of a % curriculum revision', rev_status;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_activity_resource_immutable ON activity_resource;
CREATE TRIGGER trg_activity_resource_immutable
BEFORE INSERT OR UPDATE OR DELETE ON activity_resource
FOR EACH ROW
EXECUTE FUNCTION trg_check_activity_resource_immutable();

-- 9. Curriculum Delete Protection when Published Revision Exists
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
