ALTER TABLE agent_jobs
    ADD COLUMN IF NOT EXISTS request_payload JSONB;
