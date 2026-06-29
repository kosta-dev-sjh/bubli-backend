ALTER TABLE agent_suggestions
    ADD COLUMN IF NOT EXISTS reviewed_by UUID;

ALTER TABLE agent_suggestions
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ;
