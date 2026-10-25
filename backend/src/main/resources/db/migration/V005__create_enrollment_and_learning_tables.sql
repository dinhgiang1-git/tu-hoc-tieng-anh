-- =============================================================================
-- Migration: V005__create_enrollment_and_learning_tables.sql
-- Description: Creates enrollment, lesson_schedule, activity_progress,
--              learning_event, and idempotency_record tables.
-- Conventions: BE_CODE_CONVENTION.md (UUID id, snake_case, timestamptz, FK, indexes)
-- =============================================================================

CREATE TABLE IF NOT EXISTS enrollment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    curriculum_id UUID NOT NULL REFERENCES curriculum(id) ON DELETE RESTRICT,
    revision_id UUID NOT NULL REFERENCES curriculum_revision(id) ON DELETE RESTRICT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    start_date DATE NOT NULL,
    time_zone VARCHAR(64) NOT NULL,
    study_days VARCHAR(128) NOT NULL,
    daily_target_minutes INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_enrollment_status CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'ABANDONED')),
    CONSTRAINT chk_enrollment_daily_target CHECK (daily_target_minutes >= 10 AND daily_target_minutes <= 180)
);

CREATE INDEX IF NOT EXISTS idx_enrollment_user_id ON enrollment (user_id);
CREATE INDEX IF NOT EXISTS idx_enrollment_curriculum_id ON enrollment (curriculum_id);
CREATE INDEX IF NOT EXISTS idx_enrollment_revision_id ON enrollment (revision_id);

-- Enforce at most one ACTIVE or PAUSED enrollment per user per curriculum concurrently
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_curriculum_active_idx
ON enrollment (user_id, curriculum_id)
WHERE status IN ('ACTIVE', 'PAUSED');

CREATE TABLE IF NOT EXISTS lesson_schedule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id UUID NOT NULL REFERENCES enrollment(id) ON DELETE CASCADE,
    lesson_id UUID NOT NULL REFERENCES lesson(id) ON DELETE RESTRICT,
    planned_date DATE NOT NULL,
    original_date DATE NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_enrollment_lesson_idx
ON lesson_schedule (enrollment_id, lesson_id);

CREATE INDEX IF NOT EXISTS idx_lesson_schedule_enrollment_planned_date
ON lesson_schedule (enrollment_id, planned_date);

CREATE TABLE IF NOT EXISTS activity_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id UUID NOT NULL REFERENCES enrollment(id) ON DELETE CASCADE,
    activity_id UUID NOT NULL REFERENCES activity(id) ON DELETE RESTRICT,
    status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED',
    completed_at TIMESTAMPTZ,
    user_response TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_activity_progress_status CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_enrollment_activity_progress_idx
ON activity_progress (enrollment_id, activity_id);

CREATE INDEX IF NOT EXISTS idx_activity_progress_enrollment_status
ON activity_progress (enrollment_id, status);

CREATE TABLE IF NOT EXISTS learning_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    enrollment_id UUID NOT NULL REFERENCES enrollment(id) ON DELETE CASCADE,
    activity_id UUID REFERENCES activity(id) ON DELETE SET NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_learning_event_enrollment_occurred
ON learning_event (enrollment_id, occurred_at);

CREATE TABLE IF NOT EXISTS idempotency_record (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    operation VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status_code INT NOT NULL,
    response_body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_idempotency_scope_idx
ON idempotency_record (user_id, operation, idempotency_key);

ALTER TABLE learner_preferences
ADD CONSTRAINT fk_learner_preferences_focused_enrollment
FOREIGN KEY (focused_enrollment_id) REFERENCES enrollment(id) ON DELETE SET NULL;
