-- V1 is the team baseline schema.
-- The old Step 2 migration created the legacy Document/AgentRequest model, but
-- the current schema is Resource/AgentJob based and is already present in V1.
-- Keep this version as a no-op so Flyway history remains sequential.
SELECT 1;
