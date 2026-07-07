ALTER TABLE agent_jobs
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(300);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_jobs_idempotency_key
    ON agent_jobs (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
