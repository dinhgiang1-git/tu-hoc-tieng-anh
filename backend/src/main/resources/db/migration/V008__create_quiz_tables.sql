-- =============================================================================
-- Migration: V008__create_quiz_tables.sql
-- Description: Creates quiz_question, quiz_option, quiz_attempt, and
--              quiz_attempt_answer tables for G5 assessment module.
-- Conventions: BE_CODE_CONVENTION.md (UUID id, snake_case, timestamptz, FK, indexes)
-- =============================================================================

-- 1. Quiz Question (Câu hỏi trắc nghiệm của activity QUIZ)
CREATE TABLE IF NOT EXISTS quiz_question (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    activity_id UUID NOT NULL REFERENCES activity(id) ON DELETE CASCADE,
    order_index INT NOT NULL,
    prompt TEXT NOT NULL,
    explanation TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_quiz_question_order UNIQUE (activity_id, order_index)
);

CREATE INDEX IF NOT EXISTS idx_quiz_question_activity_id ON quiz_question (activity_id);

-- 2. Quiz Option (Lựa chọn đáp án của câu hỏi trắc nghiệm)
CREATE TABLE IF NOT EXISTS quiz_option (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES quiz_question(id) ON DELETE CASCADE,
    order_index INT NOT NULL,
    content TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_quiz_option_order UNIQUE (question_id, order_index)
);

CREATE INDEX IF NOT EXISTS idx_quiz_option_question_id ON quiz_option (question_id);

-- 3. Quiz Attempt (Lượt làm bài quiz của người học)
CREATE TABLE IF NOT EXISTS quiz_attempt (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id UUID NOT NULL REFERENCES enrollment(id) ON DELETE CASCADE,
    activity_id UUID NOT NULL REFERENCES activity(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    attempt_number INT NOT NULL,
    score INT NOT NULL,
    total_questions INT NOT NULL,
    score_percentage INT NOT NULL,
    passed BOOLEAN NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_quiz_attempt_percentage CHECK (score_percentage >= 0 AND score_percentage <= 100)
);

CREATE INDEX IF NOT EXISTS idx_quiz_attempt_enrollment_activity
ON quiz_attempt (enrollment_id, activity_id, submitted_at DESC);

CREATE INDEX IF NOT EXISTS idx_quiz_attempt_user_id ON quiz_attempt (user_id);

-- 4. Quiz Attempt Answer (Chi tiết câu trả lời từng câu trong lượt làm bài)
CREATE TABLE IF NOT EXISTS quiz_attempt_answer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL REFERENCES quiz_attempt(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES quiz_question(id) ON DELETE CASCADE,
    selected_option_id UUID NOT NULL REFERENCES quiz_option(id) ON DELETE CASCADE,
    is_correct BOOLEAN NOT NULL,
    CONSTRAINT uq_quiz_attempt_question UNIQUE (attempt_id, question_id)
);

CREATE INDEX IF NOT EXISTS idx_quiz_attempt_answer_attempt_id ON quiz_attempt_answer (attempt_id);
