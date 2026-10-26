\set ON_ERROR_STOP on
BEGIN;
CREATE TEMP TABLE e2e_fixture_users (username VARCHAR(50) PRIMARY KEY) ON COMMIT DROP;
INSERT INTO e2e_fixture_users VALUES
  ('e2e_learner_m1_1'),
  ('e2e_learner_m1_2'),
  ('e2e_learner_m1_3'),
  ('e2e_learner_m1_4'),
  ('e2e_learner_m1_5'),
  ('e2e_learner_m1_6'),
  ('e2e_learner_iso_a'),
  ('e2e_learner_iso_b'),
  ('e2e_learner_ctx_a'),
  ('e2e_learner_ctx_b'),
  ('e2e_learner_tabs_act'),
  ('e2e_learner_tabs_note'),
  ('e2e_learner_autosave'),
  ('e2e_learner_focus'),
  ('e2e_learner_blocker'),
  ('e2e_learner_ux'),
  ('e2e_learner_race');

-- =============================================================================
-- E2E Fixture Seed & Reset Script for G3-04 (Dev Database Safe)
-- STRICT RULE: Only operates on entities prefixed with 'e2e_'
-- NEVER modifies or deletes personal accounts like 'dinhgiang1'
-- =============================================================================

-- 1. Insert test accounts with prefix e2e_
INSERT INTO app_user (id, username, password_hash, display_name, email, role, is_active, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'e2e_learner_m1_1', '$2a$10$0LGLN.G1bnyjLtAcyC/Ene0wkfTutzPNZtsXEdKv5DOfWxSpPg2wW', 'Học viên E2E M1-1', 'e2e_m1_1@example.com', 'LEARNER', true, NOW(), NOW()),
  (gen_random_uuid(), 'e2e_learner_m1_2', '$2a$10$0LGLN.G1bnyjLtAcyC/Ene0wkfTutzPNZtsXEdKv5DOfWxSpPg2wW', 'Học viên E2E M1-2', 'e2e_m1_2@example.com', 'LEARNER', true, NOW(), NOW()),
  (gen_random_uuid(), 'e2e_learner_m1_3', '$2a$10$0LGLN.G1bnyjLtAcyC/Ene0wkfTutzPNZtsXEdKv5DOfWxSpPg2wW', 'Học viên E2E M1-3', 'e2e_m1_3@example.com', 'LEARNER', true, NOW(), NOW()),
  (gen_random_uuid(), 'e2e_learner_m1_4', '$2a$10$0LGLN.G1bnyjLtAcyC/Ene0wkfTutzPNZtsXEdKv5DOfWxSpPg2wW', 'Học viên E2E M1-4', 'e2e_m1_4@example.com', 'LEARNER', true, NOW(), NOW()),
  (gen_random_uuid(), 'e2e_learner_m1_5', '$2a$10$0LGLN.G1bnyjLtAcyC/Ene0wkfTutzPNZtsXEdKv5DOfWxSpPg2wW', 'Học viên E2E M1-5', 'e2e_m1_5@example.com', 'LEARNER', true, NOW(), NOW()),
  (gen_random_uuid(), 'e2e_learner_m1_6', '$2a$10$0LGLN.G1bnyjLtAcyC/Ene0wkfTutzPNZtsXEdKv5DOfWxSpPg2wW', 'Học viên E2E M1-6', 'e2e_m1_6@example.com', 'LEARNER', true, NOW(), NOW()),
  (gen_random_uuid(), 'e2e_learner_iso_a', '$2a$10$0LGLN.G1bnyjLtAcyC/Ene0wkfTutzPNZtsXEdKv5DOfWxSpPg2wW', 'Học viên E2E Iso A', 'e2e_iso_a@example.com', 'LEARNER', true, NOW(), NOW()),
  (gen_random_uuid(), 'e2e_learner_iso_b', '$2a$10$0LGLN.G1bnyjLtAcyC/Ene0wkfTutzPNZtsXEdKv5DOfWxSpPg2wW', 'Học viên E2E Iso B', 'e2e_iso_b@example.com', 'LEARNER', true, NOW(), NOW()),
  (gen_random_uuid(), 'e2e_learner_ctx_a', '$2a$10$0LGLN.G1bnyjLtAcyC/Ene0wkfTutzPNZtsXEdKv5DOfWxSpPg2wW', 'Học viên E2E Ctx A', 'e2e_ctx_a@example.com', 'LEARNER', true, NOW(), NOW()),
  (gen_random_uuid(), 'e2e_learner_ctx_b', '$2a$10$0LGLN.G1bnyjLtAcyC/Ene0wkfTutzPNZtsXEdKv5DOfWxSpPg2wW', 'Học viên E2E Ctx B', 'e2e_ctx_b@example.com', 'LEARNER', true, NOW(), NOW()),
  (gen_random_uuid(), 'e2e_learner_tabs_act', '$2a$10$0LGLN.G1bnyjLtAcyC/Ene0wkfTutzPNZtsXEdKv5DOfWxSpPg2wW', 'Học viên E2E Tabs Act', 'e2e_tabs_act@example.com', 'LEARNER', true, NOW(), NOW()),
  (gen_random_uuid(), 'e2e_learner_tabs_note', '$2a$10$0LGLN.G1bnyjLtAcyC/Ene0wkfTutzPNZtsXEdKv5DOfWxSpPg2wW', 'Học viên E2E Tabs Note', 'e2e_tabs_note@example.com', 'LEARNER', true, NOW(), NOW()),
  (gen_random_uuid(), 'e2e_learner_autosave', '$2a$10$0LGLN.G1bnyjLtAcyC/Ene0wkfTutzPNZtsXEdKv5DOfWxSpPg2wW', 'Học viên E2E Autosave', 'e2e_autosave@example.com', 'LEARNER', true, NOW(), NOW()),
  (gen_random_uuid(), 'e2e_learner_focus', '$2a$10$0LGLN.G1bnyjLtAcyC/Ene0wkfTutzPNZtsXEdKv5DOfWxSpPg2wW', 'Học viên E2E Focus', 'e2e_focus@example.com', 'LEARNER', true, NOW(), NOW()),
  (gen_random_uuid(), 'e2e_learner_blocker', '$2a$10$0LGLN.G1bnyjLtAcyC/Ene0wkfTutzPNZtsXEdKv5DOfWxSpPg2wW', 'Học viên E2E Blocker', 'e2e_blocker@example.com', 'LEARNER', true, NOW(), NOW()),
  (gen_random_uuid(), 'e2e_learner_ux', '$2a$10$0LGLN.G1bnyjLtAcyC/Ene0wkfTutzPNZtsXEdKv5DOfWxSpPg2wW', 'Học viên E2E UX', 'e2e_ux@example.com', 'LEARNER', true, NOW(), NOW()),
  (gen_random_uuid(), 'e2e_learner_race', '$2a$10$0LGLN.G1bnyjLtAcyC/Ene0wkfTutzPNZtsXEdKv5DOfWxSpPg2wW', 'Học viên E2E Race', 'e2e_race@example.com', 'LEARNER', true, NOW(), NOW())
ON CONFLICT (username) DO UPDATE SET password_hash = EXCLUDED.password_hash,
  display_name = EXCLUDED.display_name, email = EXCLUDED.email;

-- 2. Insert learner preferences
INSERT INTO learner_preferences (user_id, daily_goal_minutes, study_days, timezone, updated_at)
SELECT id, 30, 'MON,WED,FRI', 'Asia/Ho_Chi_Minh', NOW()
FROM app_user
WHERE username IN (SELECT username FROM e2e_fixture_users)
ON CONFLICT (user_id) DO NOTHING;

-- 3. Reset any state created by previous E2E test runs (scoped exclusively to e2e_% users)
DELETE FROM note WHERE user_id IN (SELECT id FROM app_user WHERE username IN (SELECT username FROM e2e_fixture_users));
DELETE FROM activity_progress WHERE enrollment_id IN (SELECT id FROM enrollment WHERE user_id IN (SELECT id FROM app_user WHERE username IN (SELECT username FROM e2e_fixture_users)));
DELETE FROM learning_event WHERE user_id IN (SELECT id FROM app_user WHERE username IN (SELECT username FROM e2e_fixture_users));
DELETE FROM idempotency_record WHERE user_id IN (SELECT id FROM app_user WHERE username IN (SELECT username FROM e2e_fixture_users));
DELETE FROM enrollment WHERE user_id IN (SELECT id FROM app_user WHERE username IN (SELECT username FROM e2e_fixture_users));
UPDATE learner_preferences SET focused_enrollment_id = NULL WHERE user_id IN (SELECT id FROM app_user WHERE username IN (SELECT username FROM e2e_fixture_users));

-- 4. Recreate secondary curriculum fixture for M1-6 cross-revision isolation testing
DO $$
DECLARE
  v_curriculum_id UUID := 'c0000000-0000-0000-0000-000000000002'::uuid;
  v_revision_id UUID := 'a0000000-0000-0000-0000-000000000002'::uuid;
  v_phase_id UUID := 'e2e00000-0000-0000-0000-000000000001'::uuid;
  v_week_id UUID := 'e2e00000-0000-0000-0000-000000000002'::uuid;
  v_lesson_id UUID := 'e2e00000-0000-0000-0000-000000000003'::uuid;
  v_activity_id UUID := 'e2e00000-0000-0000-0000-000000000004'::uuid;
BEGIN
  -- Reuse the immutable published fixture; never disable triggers or delete its tree.
  IF EXISTS (SELECT 1 FROM curriculum WHERE curriculum_key = 'e2e_other_curriculum') THEN
    IF NOT EXISTS (SELECT 1 FROM activity a JOIN lesson l ON l.id = a.lesson_id
        JOIN curriculum_week w ON w.id = l.week_id
        JOIN curriculum_phase p ON p.id = w.phase_id
        JOIN curriculum_revision r ON r.id = p.revision_id
        JOIN curriculum c ON c.id = r.curriculum_id
        WHERE a.id = v_activity_id AND r.id = v_revision_id AND c.curriculum_key = 'e2e_other_curriculum') THEN
      RAISE EXCEPTION 'Existing E2E curriculum fixture does not match expected IDs';
    END IF;
    RETURN;
  END IF;
  INSERT INTO curriculum (id, curriculum_key, target_locale, created_at, updated_at)
  VALUES (v_curriculum_id, 'e2e_other_curriculum', 'en-GB', NOW(), NOW());

  INSERT INTO curriculum_revision (id, curriculum_id, version, status, title, description, authors, provenance, checksum, created_at, updated_at)
  VALUES (
    v_revision_id,
    v_curriculum_id,
    '1.0.0',
    'DRAFT',
    'Giáo trình khác E2E',
    'Dùng để kiểm thử phân tách revision',
    '["Antigravity E2E"]',
    '{"sourceName":"E2E Source","sourceUrl":"https://example.com","sourceLicense":"CC0","authorCompiled":true,"verificationStatus":"VERIFIED"}',
    'chk_e2e_other_100',
    NOW(),
    NOW()
  );

  INSERT INTO curriculum_phase (id, revision_id, phase_key, title, order_index)
  VALUES (v_phase_id, v_revision_id, 'e2e_other.phase01', 'Giai đoạn khác', 1);

  INSERT INTO curriculum_week (id, phase_id, week_key, week_number, order_index, title, description)
  VALUES (v_week_id, v_phase_id, 'e2e_other.w01', 1, 1, 'Tuần khác', 'Mô tả tuần khác');

  INSERT INTO lesson (id, week_id, lesson_key, order_index, title, estimated_minutes, is_required, content_markdown)
  VALUES (v_lesson_id, v_week_id, 'e2e_other.w01.l01', 1, 'Bài học từ revision khác', 15, true, 'Nội dung bài khác');

  INSERT INTO activity (id, lesson_id, activity_key, order_index, title, activity_type, is_required, completion_policy, instructions)
  VALUES (v_activity_id, v_lesson_id, 'e2e_other.w01.l01.a01', 1, 'Nhiệm vụ ngoài lộ trình', 'READING', true, 'MANUAL_CHECK', 'Hướng dẫn nhiệm vụ khác');

  UPDATE curriculum_revision
  SET status = 'PUBLISHED', published_at = NOW(), updated_at = NOW()
  WHERE id = v_revision_id;
END $$;

COMMIT;
