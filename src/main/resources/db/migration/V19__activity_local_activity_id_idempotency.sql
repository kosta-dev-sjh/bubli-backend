ALTER TABLE activity_logs
    ADD COLUMN local_activity_id VARCHAR(120);

CREATE UNIQUE INDEX ux_activity_logs_user_local_activity_id
    ON activity_logs(user_id, local_activity_id)
    WHERE local_activity_id IS NOT NULL;
