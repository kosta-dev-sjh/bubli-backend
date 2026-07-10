-- Bubli demo account activity seed.
-- Target: Docker Postgres database created by this backend repo.
-- Purpose: 365 days of time_logs (heatmap) + activity_logs (app usage stats)
--          for the demo account so dashboard/heatmap screens have data to show.
--
-- Demo account: bubliteam1234@gmail.com
-- users.bubli_id has no email column, so the account is resolved via its
-- Bubli ID suffix shown in 설정 > 계정 (표시된 값: -qbnt3130).
--
-- Activity coverage: not every day has data.
--   - Weekdays: 40% chance of no activity at all that day.
--   - Weekends (Sat/Sun): 70% chance of no activity at all that day.
--   A day with no activity has neither time_logs nor activity_logs rows.
--
-- Idempotent: previously seeded rows for the demo user
-- (user_id = '4c495add-d066-4623-87da-4d8ed9e457d7') are deleted before
-- inserting the new 365-day set.

BEGIN;

SET LOCAL TIME ZONE 'Asia/Seoul';

-- Fail fast if the demo account can't be found, instead of silently inserting 0 rows.
DO $$
DECLARE
    v_user_id UUID;
BEGIN
    SELECT id INTO v_user_id
    FROM users
    WHERE bubli_id LIKE '%qbnt3130'
    LIMIT 1;

    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Demo user not found (users.bubli_id LIKE ''%%qbnt3130'')';
    END IF;
END $$;

-- 0) Clean up previously seeded rows for the demo user before reseeding.
DELETE FROM activity_logs WHERE user_id = '4c495add-d066-4623-87da-4d8ed9e457d7';
DELETE FROM time_logs WHERE user_id = '4c495add-d066-4623-87da-4d8ed9e457d7';

-- Shared per-day "did the user show up at all" roll, reused by both
-- time_logs and activity_logs below so a no-activity day has no rows
-- in either table.
CREATE TEMP TABLE demo_seed_active_days ON COMMIT DROP AS
SELECT day_offset
FROM generate_series(0, 364) AS day_offset
WHERE
    CASE
        WHEN EXTRACT(DOW FROM (CURRENT_DATE - day_offset)) IN (0, 6)
            THEN random() < 0.3  -- weekend: 70% no-activity -> keep 30%
        ELSE random() < 0.6      -- weekday: 40% no-activity -> keep 60%
    END;

-- 1) time_logs: active days only, 1~3 timers/day, 30min~2h each, 09:00~20:00 KST, ENDED
WITH target_user AS (
    SELECT id AS user_id
    FROM users
    WHERE bubli_id LIKE '%qbnt3130'
    LIMIT 1
),
day_log_counts AS (
    SELECT day_offset, (1 + floor(random() * 3))::int AS log_count
    FROM demo_seed_active_days
),
day_logs AS (
    SELECT dlc.day_offset, gs AS seq
    FROM day_log_counts dlc
    CROSS JOIN LATERAL generate_series(1, dlc.log_count) AS gs
),
time_log_seed AS (
    SELECT
        dl.day_offset,
        dl.seq,
        (1800 + floor(random() * (7200 - 1800 + 1)))::bigint AS duration_seconds
    FROM day_logs dl
)
INSERT INTO time_logs (
    id, user_id, room_id, timer_type, idempotency_key, recovered_from_time_log_id,
    status, started_at, last_started_at, ended_at, duration_seconds, last_heartbeat_at,
    created_at, updated_at
)
SELECT
    gen_random_uuid(),
    tu.user_id,
    NULL::uuid,
    'WORK',
    'seed-' || gen_random_uuid()::text,
    NULL::uuid,
    'ENDED',
    win.started_at,
    win.started_at,
    win.ended_at,
    tls.duration_seconds,
    win.ended_at,
    win.started_at,
    win.ended_at
FROM time_log_seed tls
CROSS JOIN target_user tu
CROSS JOIN LATERAL (
    SELECT
        (CURRENT_DATE - tls.day_offset)::timestamp
            + TIME '09:00:00'
            + (floor(random() * (39600 - tls.duration_seconds)) || ' seconds')::interval AS started_at
) AS start_point
CROSS JOIN LATERAL (
    SELECT
        start_point.started_at AS started_at,
        start_point.started_at + (tls.duration_seconds || ' seconds')::interval AS ended_at
) AS win;

-- 2) activity_logs: active days only, 3~5 distinct apps/day, 10min~90min each, 09:00~20:00 KST
WITH target_user AS (
    SELECT id AS user_id
    FROM users
    WHERE bubli_id LIKE '%qbnt3130'
    LIMIT 1
),
apps (app_name, window_title) AS (
    VALUES
        ('VS Code', 'bubli-backend - TimeLogService.java'),
        ('Figma', '랜딩 페이지 시안'),
        ('Chrome', 'Gmail - 받은편지함'),
        ('Slack', '#general'),
        ('Notion', '주간 회의록'),
        ('Postman', 'API 테스트 - /api/time-logs')
),
day_app_counts AS (
    SELECT day_offset, (3 + floor(random() * 3))::int AS app_count
    FROM demo_seed_active_days
),
day_apps AS (
    SELECT
        dac.day_offset,
        dac.app_count,
        a.app_name,
        a.window_title,
        ROW_NUMBER() OVER (PARTITION BY dac.day_offset ORDER BY random()) AS rn
    FROM day_app_counts dac
    CROSS JOIN apps a
),
activity_log_seed AS (
    SELECT
        da.day_offset,
        da.app_name,
        da.window_title,
        (600 + floor(random() * (5400 - 600 + 1)))::bigint AS duration_seconds
    FROM day_apps da
    WHERE da.rn <= da.app_count
)
INSERT INTO activity_logs (
    id, user_id, room_id, app_name, window_title, started_at, ended_at, duration_seconds, created_at
)
SELECT
    gen_random_uuid(),
    tu.user_id,
    NULL::uuid,
    als.app_name,
    als.window_title,
    win.started_at,
    win.ended_at,
    als.duration_seconds,
    win.started_at
FROM activity_log_seed als
CROSS JOIN target_user tu
CROSS JOIN LATERAL (
    SELECT
        (CURRENT_DATE - als.day_offset)::timestamp
            + TIME '09:00:00'
            + (floor(random() * (39600 - als.duration_seconds)) || ' seconds')::interval AS started_at
) AS start_point
CROSS JOIN LATERAL (
    SELECT
        start_point.started_at AS started_at,
        start_point.started_at + (als.duration_seconds || ' seconds')::interval AS ended_at
) AS win;

COMMIT;
