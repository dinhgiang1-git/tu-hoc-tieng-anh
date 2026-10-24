-- =============================================================================
-- Migration: V002__add_user_role_and_preferences_alignment.sql
-- Description: Adds role to app_user and aligns preferences daily_goal_minutes (10 to 180)
-- =============================================================================

ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'LEARNER';

ALTER TABLE learner_preferences
    DROP CONSTRAINT IF EXISTS chk_daily_goal_minutes;

ALTER TABLE learner_preferences
    ADD CONSTRAINT chk_daily_goal_minutes CHECK (daily_goal_minutes >= 10 AND daily_goal_minutes <= 180);
