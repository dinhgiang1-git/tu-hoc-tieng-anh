-- =============================================================================
-- Migration: V003__create_curriculum_tables.sql
-- Description: Creates relational schema for curriculum, revision, phase,
--              curriculum_week, lesson, activity, resource, and activity_resource
-- Conventions: BE_CODE_CONVENTION.md, ADR-005 (UUID id, snake_case, timestamptz, FK)
-- =============================================================================

-- 1. Curriculum (Lộ trình cấp cao)
CREATE TABLE IF NOT EXISTS curriculum (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    curriculum_key VARCHAR(50) NOT NULL UNIQUE,
    target_locale VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_curriculum_key ON curriculum (curriculum_key);

-- 2. Curriculum Revision (Phiên bản lộ trình)
CREATE TABLE IF NOT EXISTS curriculum_revision (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    curriculum_id UUID NOT NULL REFERENCES curriculum(id) ON DELETE CASCADE,
    version VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    authors TEXT NOT NULL,
    provenance TEXT NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_curriculum_revision_version UNIQUE (curriculum_id, version),
    CONSTRAINT chk_curriculum_revision_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE INDEX IF NOT EXISTS idx_curriculum_revision_curriculum_id ON curriculum_revision (curriculum_id);
CREATE INDEX IF NOT EXISTS idx_curriculum_revision_status ON curriculum_revision (status);

-- 3. Phase (Giai đoạn học tập)
CREATE TABLE IF NOT EXISTS curriculum_phase (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    revision_id UUID NOT NULL REFERENCES curriculum_revision(id) ON DELETE CASCADE,
    phase_key VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    order_index INT NOT NULL,
    CONSTRAINT uq_curriculum_phase_key UNIQUE (revision_id, phase_key),
    CONSTRAINT uq_curriculum_phase_order UNIQUE (revision_id, order_index)
);

CREATE INDEX IF NOT EXISTS idx_curriculum_phase_revision_id ON curriculum_phase (revision_id);

-- 4. Curriculum Week (Tuần nội dung sư phạm)
CREATE TABLE IF NOT EXISTS curriculum_week (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phase_id UUID NOT NULL REFERENCES curriculum_phase(id) ON DELETE CASCADE,
    week_key VARCHAR(50) NOT NULL,
    week_number INT NOT NULL,
    order_index INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    CONSTRAINT uq_curriculum_week_key UNIQUE (phase_id, week_key),
    CONSTRAINT uq_curriculum_week_order UNIQUE (phase_id, order_index)
);

CREATE INDEX IF NOT EXISTS idx_curriculum_week_phase_id ON curriculum_week (phase_id);

-- 5. Lesson (Buổi học)
CREATE TABLE IF NOT EXISTS lesson (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    week_id UUID NOT NULL REFERENCES curriculum_week(id) ON DELETE CASCADE,
    lesson_key VARCHAR(50) NOT NULL,
    order_index INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    estimated_minutes INT NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    content_markdown TEXT NOT NULL,
    CONSTRAINT uq_lesson_key UNIQUE (week_id, lesson_key),
    CONSTRAINT uq_lesson_order UNIQUE (week_id, order_index),
    CONSTRAINT chk_lesson_estimated_minutes CHECK (estimated_minutes >= 5 AND estimated_minutes <= 180)
);

CREATE INDEX IF NOT EXISTS idx_lesson_week_id ON lesson (week_id);

-- 6. Activity (Nhiệm vụ học tập)
CREATE TABLE IF NOT EXISTS activity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id UUID NOT NULL REFERENCES lesson(id) ON DELETE CASCADE,
    activity_key VARCHAR(50) NOT NULL,
    order_index INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    activity_type VARCHAR(30) NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    completion_policy VARCHAR(30) NOT NULL,
    instructions TEXT NOT NULL,
    skill_tags TEXT,
    CONSTRAINT uq_activity_key UNIQUE (lesson_id, activity_key),
    CONSTRAINT uq_activity_order UNIQUE (lesson_id, order_index),
    CONSTRAINT chk_activity_type CHECK (activity_type IN (
        'READING', 'EXTERNAL_RESOURCE', 'PRACTICE', 'REFLECTION', 'QUIZ', 'DICTATION', 'RECORDING'
    )),
    CONSTRAINT chk_completion_policy CHECK (completion_policy IN (
        'MANUAL_CHECK', 'MANUAL_CHECK_WITH_RESPONSE', 'RESPONSE_SUBMIT', 'ON_SUBMIT', 'PASS_SCORE'
    ))
);

CREATE INDEX IF NOT EXISTS idx_activity_lesson_id ON activity (lesson_id);

-- 7. Resource (Tài nguyên tham khảo)
CREATE TABLE IF NOT EXISTS resource (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    revision_id UUID NOT NULL REFERENCES curriculum_revision(id) ON DELETE CASCADE,
    resource_key VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    url TEXT NOT NULL,
    media_type VARCHAR(20) NOT NULL,
    target_locale VARCHAR(10) NOT NULL,
    attribution TEXT,
    license TEXT,
    verification_status VARCHAR(30) NOT NULL,
    CONSTRAINT uq_resource_key UNIQUE (revision_id, resource_key),
    CONSTRAINT chk_resource_media_type CHECK (media_type IN ('VIDEO', 'AUDIO', 'ARTICLE', 'PDF')),
    CONSTRAINT chk_resource_verification_status CHECK (verification_status IN (
        'UNVERIFIED_DRAFT', 'LINK_CHECKED', 'PEDAGOGICALLY_REVIEWED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_resource_revision_id ON resource (revision_id);

-- 8. Activity Resource Link (Liên kết nhiều-nhiều giữa nhiệm vụ và tài nguyên)
CREATE TABLE IF NOT EXISTS activity_resource (
    activity_id UUID NOT NULL REFERENCES activity(id) ON DELETE CASCADE,
    resource_id UUID NOT NULL REFERENCES resource(id) ON DELETE CASCADE,
    PRIMARY KEY (activity_id, resource_id)
);

CREATE INDEX IF NOT EXISTS idx_activity_resource_resource_id ON activity_resource (resource_id);

-- 9. Immutability Trigger for PUBLISHED Revisions
CREATE OR REPLACE FUNCTION trg_check_revision_published_immutable()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status = 'PUBLISHED' AND NEW.status = 'PUBLISHED' THEN
        IF OLD.title <> NEW.title
           OR OLD.description <> NEW.description
           OR OLD.authors <> NEW.authors
           OR OLD.provenance <> NEW.provenance
           OR OLD.checksum <> NEW.checksum
           OR OLD.version <> NEW.version
           OR OLD.curriculum_id <> NEW.curriculum_id THEN
            RAISE EXCEPTION 'PUBLISHED curriculum revision is immutable and cannot be modified';
        END IF;
    END IF;
    IF OLD.status = 'PUBLISHED' AND NEW.status = 'DRAFT' THEN
        RAISE EXCEPTION 'PUBLISHED curriculum revision cannot be reverted to DRAFT';
    END IF;
    IF OLD.status = 'ARCHIVED' AND NEW.status <> 'ARCHIVED' THEN
        RAISE EXCEPTION 'ARCHIVED curriculum revision cannot change status';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_curriculum_revision_immutable
BEFORE UPDATE ON curriculum_revision
FOR EACH ROW
EXECUTE FUNCTION trg_check_revision_published_immutable();
