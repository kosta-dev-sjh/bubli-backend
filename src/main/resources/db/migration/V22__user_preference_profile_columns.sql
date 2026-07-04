ALTER TABLE user_preferences
    ADD COLUMN job_role VARCHAR(40),
    ADD COLUMN onboarding_completed_at TIMESTAMPTZ;
