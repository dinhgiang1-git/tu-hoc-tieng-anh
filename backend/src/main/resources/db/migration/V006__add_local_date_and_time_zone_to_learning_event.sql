-- V006: Add local_date and time_zone to learning_event
-- Purpose: Persist both UTC occurred_at and local date in the enrollment's timezone at the moment of the learning action.

ALTER TABLE learning_event
    ADD COLUMN IF NOT EXISTS local_date DATE,
    ADD COLUMN IF NOT EXISTS time_zone VARCHAR(64);

-- Backfill existing rows (if any) from associated enrollment timezone
UPDATE learning_event le
SET time_zone = e.time_zone,
    local_date = (le.occurred_at AT TIME ZONE e.time_zone)::date
FROM enrollment e
WHERE le.enrollment_id = e.id AND le.local_date IS NULL;

CREATE INDEX IF NOT EXISTS idx_learning_event_enrollment_local_date
ON learning_event (enrollment_id, local_date);
