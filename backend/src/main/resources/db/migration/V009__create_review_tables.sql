-- =============================================================================
-- Migration: V009__create_review_tables.sql
-- Description: Creates review_bookmark and review_resolution tables for G5-02
--              review and mistake resolution tracking module.
-- Conventions: BE_CODE_CONVENTION.md (UUID id, snake_case, timestamptz, FK, indexes)
-- =============================================================================

-- 1. Review Bookmark (Đánh dấu bài học hoặc nhiệm vụ để ôn tập)
CREATE TABLE IF NOT EXISTS review_bookmark (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    enrollment_id UUID NOT NULL REFERENCES enrollment(id) ON DELETE CASCADE,
    lesson_id UUID NOT NULL REFERENCES lesson(id) ON DELETE CASCADE,
    activity_id UUID REFERENCES activity(id) ON DELETE CASCADE,
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_review_bookmark
ON review_bookmark (enrollment_id, lesson_id, COALESCE(activity_id, '00000000-0000-0000-0000-000000000000'::uuid));

CREATE INDEX IF NOT EXISTS idx_review_bookmark_user_id ON review_bookmark (user_id);
CREATE INDEX IF NOT EXISTS idx_review_bookmark_enrollment_id ON review_bookmark (enrollment_id);

-- 2. Review Resolution (Theo dõi câu hỏi sai đã được người học giải quyết)
CREATE TABLE IF NOT EXISTS review_resolution (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id UUID NOT NULL REFERENCES enrollment(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES quiz_question(id) ON DELETE CASCADE,
    resolved_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_review_resolution UNIQUE (enrollment_id, question_id)
);

CREATE INDEX IF NOT EXISTS idx_review_resolution_enrollment_id ON review_resolution (enrollment_id);
