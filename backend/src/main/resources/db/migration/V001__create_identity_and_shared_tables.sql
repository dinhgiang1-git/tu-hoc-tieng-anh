-- =============================================================================
-- Migration: V001__create_identity_and_shared_tables.sql
-- Description: Creates identity (app_user) and learner_preferences tables
-- Conventions: BE_CODE_CONVENTION.md (UUID id, snake_case, timestamptz, FK)
-- =============================================================================

CREATE TABLE IF NOT EXISTS app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    email VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_app_user_username ON app_user (username);
CREATE INDEX IF NOT EXISTS idx_app_user_email ON app_user (email);

CREATE TABLE IF NOT EXISTS learner_preferences (
    user_id UUID PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
    daily_goal_minutes INT NOT NULL DEFAULT 30,
    study_days VARCHAR(50) NOT NULL DEFAULT 'MON,WED,FRI',
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    focused_enrollment_id UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_daily_goal_minutes CHECK (daily_goal_minutes >= 5 AND daily_goal_minutes <= 240)
);
