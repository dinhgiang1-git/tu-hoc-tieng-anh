-- V007__create_notes_table.sql
-- Module ghi chú học tập cá nhân (Notes) cho Task G3-03

CREATE TABLE note (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    enrollment_id UUID REFERENCES enrollment(id) ON DELETE CASCADE,
    lesson_id UUID REFERENCES lesson(id) ON DELETE CASCADE,
    activity_id UUID REFERENCES activity(id) ON DELETE CASCADE,
    content TEXT NOT NULL DEFAULT '',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

-- Unique partial index cho ghi chú cấp buổi học (activity_id IS NULL, lesson_id IS NOT NULL)
-- Đảm bảo mỗi (user, enrollment, lesson) chỉ tồn tại duy nhất 1 ghi chú
CREATE UNIQUE INDEX uq_note_owner_enrollment_lesson
    ON note(user_id, enrollment_id, lesson_id)
    WHERE activity_id IS NULL AND lesson_id IS NOT NULL;

-- Index hỗ trợ phân trang danh sách ghi chú cá nhân theo thời gian cập nhật
CREATE INDEX idx_note_user_updated
    ON note(user_id, updated_at DESC, id DESC);

-- Index hỗ trợ lọc ghi chú theo enrollment và lesson
CREATE INDEX idx_note_user_enrollment_lesson
    ON note(user_id, enrollment_id, lesson_id);
