WITH ranked_running_time_logs AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY user_id
            ORDER BY
                COALESCE(last_heartbeat_at, last_started_at, started_at, created_at) DESC,
                created_at DESC,
                id DESC
        ) AS row_no
    FROM time_logs
    WHERE status = 'RUNNING'
)
UPDATE time_logs time_log
SET
    status = 'NEEDS_RECOVERY',
    updated_at = CURRENT_TIMESTAMP
FROM ranked_running_time_logs ranked
WHERE time_log.id = ranked.id
  AND ranked.row_no > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_time_logs_user_running
    ON time_logs (user_id)
    WHERE status = 'RUNNING';
