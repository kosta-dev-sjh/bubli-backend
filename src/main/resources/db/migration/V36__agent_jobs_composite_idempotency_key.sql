DROP INDEX IF EXISTS uk_agent_jobs_idempotency_key;

CREATE UNIQUE INDEX uk_agent_jobs_idempotency_key
    ON agent_jobs (requested_by_user_id, job_type, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
