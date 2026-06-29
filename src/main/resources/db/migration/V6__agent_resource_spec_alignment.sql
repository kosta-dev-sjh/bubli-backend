-- Align the V1 baseline schema with the current Resource/AgentJob model.

ALTER TABLE resources
    ALTER COLUMN title TYPE VARCHAR(255),
    ALTER COLUMN kind TYPE VARCHAR(20),
    ALTER COLUMN visibility TYPE VARCHAR(20),
    ALTER COLUMN status TYPE VARCHAR(20);

ALTER TABLE resources
    DROP CONSTRAINT IF EXISTS ck_resources_kind,
    DROP CONSTRAINT IF EXISTS ck_resources_visibility,
    DROP CONSTRAINT IF EXISTS ck_resources_status,
    DROP CONSTRAINT IF EXISTS ck_resources_room_shared;

ALTER TABLE resources
    ADD CONSTRAINT ck_resources_kind
        CHECK (kind IN ('FILE', 'MEMO')),
    ADD CONSTRAINT ck_resources_visibility
        CHECK (visibility IN ('PERSONAL', 'ROOM_SHARED')),
    ADD CONSTRAINT ck_resources_status
        CHECK (status IN ('UPLOADING', 'READY', 'ANALYZING', 'ANALYZED', 'FAILED')),
    ADD CONSTRAINT ck_resources_room_shared
        CHECK (visibility <> 'ROOM_SHARED' OR room_id IS NOT NULL);

CREATE INDEX IF NOT EXISTS idx_resources_owner_visibility
    ON resources (owner_id, visibility);
CREATE INDEX IF NOT EXISTS idx_resources_room_status
    ON resources (room_id, status);

ALTER TABLE resource_files
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

UPDATE resource_files
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE resource_files
    ALTER COLUMN updated_at SET NOT NULL,
    ALTER COLUMN storage_key TYPE VARCHAR(1000),
    ALTER COLUMN mime_type TYPE VARCHAR(100),
    ALTER COLUMN checksum TYPE VARCHAR(64);

ALTER TABLE resource_files
    DROP CONSTRAINT IF EXISTS fk_resource_files_resource,
    DROP CONSTRAINT IF EXISTS ck_resource_files_size;

ALTER TABLE resource_files
    ADD CONSTRAINT fk_resource_files_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE CASCADE,
    ADD CONSTRAINT ck_resource_files_size
        CHECK (size_bytes >= 0);

CREATE INDEX IF NOT EXISTS idx_resource_files_resource
    ON resource_files (resource_id);
CREATE INDEX IF NOT EXISTS idx_resource_files_checksum
    ON resource_files (checksum);
CREATE UNIQUE INDEX IF NOT EXISTS uk_resource_files_storage_key
    ON resource_files (storage_key);

ALTER TABLE resource_versions
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

UPDATE resource_versions
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE resource_versions
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE resource_versions
    DROP CONSTRAINT IF EXISTS fk_resource_versions_resource,
    DROP CONSTRAINT IF EXISTS fk_resource_versions_file,
    DROP CONSTRAINT IF EXISTS ck_resource_versions_version;

ALTER TABLE resource_versions
    ADD CONSTRAINT fk_resource_versions_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_resource_versions_file
        FOREIGN KEY (file_id) REFERENCES resource_files (id) ON DELETE CASCADE,
    ADD CONSTRAINT ck_resource_versions_version
        CHECK (version_no >= 1);

CREATE INDEX IF NOT EXISTS idx_resource_versions_resource
    ON resource_versions (resource_id);
CREATE INDEX IF NOT EXISTS idx_resource_versions_file
    ON resource_versions (file_id);

ALTER TABLE resource_summaries
    ALTER COLUMN job_id DROP NOT NULL,
    ALTER COLUMN summary_json DROP NOT NULL,
    ALTER COLUMN status TYPE VARCHAR(20),
    ALTER COLUMN prompt_version TYPE VARCHAR(50),
    ALTER COLUMN schema_version TYPE VARCHAR(50),
    ALTER COLUMN model_name TYPE VARCHAR(100);

ALTER TABLE resource_summaries
    DROP CONSTRAINT IF EXISTS fk_resource_summaries_resource,
    DROP CONSTRAINT IF EXISTS fk_resource_summaries_job,
    DROP CONSTRAINT IF EXISTS ck_resource_summaries_status;

ALTER TABLE resource_summaries
    ADD CONSTRAINT fk_resource_summaries_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE CASCADE,
    ADD CONSTRAINT ck_resource_summaries_status
        CHECK (status IN ('READY', 'ANALYZING', 'ANALYZED', 'FAILED'));

CREATE INDEX IF NOT EXISTS idx_resource_summaries_resource
    ON resource_summaries (resource_id);
CREATE INDEX IF NOT EXISTS idx_resource_summaries_job
    ON resource_summaries (job_id);

ALTER TABLE ai_documents
    ALTER COLUMN document_type TYPE VARCHAR(30),
    ALTER COLUMN status TYPE VARCHAR(20);

ALTER TABLE ai_documents
    DROP CONSTRAINT IF EXISTS fk_ai_documents_resource,
    DROP CONSTRAINT IF EXISTS ck_ai_documents_type,
    DROP CONSTRAINT IF EXISTS ck_ai_documents_status,
    DROP CONSTRAINT IF EXISTS ck_ai_documents_confidence;

ALTER TABLE ai_documents
    ADD CONSTRAINT fk_ai_documents_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE CASCADE,
    ADD CONSTRAINT ck_ai_documents_type
        CHECK (document_type IN ('CONTRACT', 'REQUIREMENT', 'MEETING_NOTE', 'REFERENCE', 'QUOTATION', 'GENERAL')),
    ADD CONSTRAINT ck_ai_documents_status
        CHECK (status IN ('READY', 'ANALYZING', 'ANALYZED', 'FAILED')),
    ADD CONSTRAINT ck_ai_documents_confidence
        CHECK (detected_confidence IS NULL OR (detected_confidence >= 0 AND detected_confidence <= 1));

CREATE INDEX IF NOT EXISTS idx_ai_documents_room_status
    ON ai_documents (room_id, status);

ALTER TABLE agent_jobs
    ALTER COLUMN status TYPE VARCHAR(20),
    ALTER COLUMN error_code TYPE VARCHAR(100),
    ALTER COLUMN error_message TYPE VARCHAR(1000);

ALTER TABLE agent_jobs
    DROP CONSTRAINT IF EXISTS fk_agent_jobs_resource,
    DROP CONSTRAINT IF EXISTS ck_agent_jobs_type,
    DROP CONSTRAINT IF EXISTS ck_agent_jobs_status,
    DROP CONSTRAINT IF EXISTS ck_agent_jobs_retry_count;

ALTER TABLE agent_jobs
    ADD CONSTRAINT fk_agent_jobs_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE SET NULL,
    ADD CONSTRAINT ck_agent_jobs_type
        CHECK (job_type IN (
            'ANALYZE_RESOURCE',
            'GENERATE_WBS',
            'GENERATE_TASKS',
            'GENERATE_REQUIREMENTS',
            'REVIEW_CONTRACT_DOCUMENTS',
            'GENERATE_QUESTIONS',
            'DAILY_SUMMARY',
            'DRAFT_DOCUMENT'
        )),
    ADD CONSTRAINT ck_agent_jobs_status
        CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELED')),
    ADD CONSTRAINT ck_agent_jobs_retry_count
        CHECK (retry_count >= 0);

CREATE INDEX IF NOT EXISTS idx_agent_jobs_room_status
    ON agent_jobs (room_id, status);
CREATE INDEX IF NOT EXISTS idx_agent_jobs_resource
    ON agent_jobs (resource_id);
CREATE INDEX IF NOT EXISTS idx_agent_jobs_requested_by
    ON agent_jobs (requested_by_user_id, created_at DESC);

ALTER TABLE resource_summaries
    ADD CONSTRAINT fk_resource_summaries_job
        FOREIGN KEY (job_id) REFERENCES agent_jobs (id) ON DELETE SET NULL;

ALTER TABLE agent_suggestions
    ALTER COLUMN status TYPE VARCHAR(20);

ALTER TABLE agent_suggestions
    DROP CONSTRAINT IF EXISTS fk_agent_suggestions_job,
    DROP CONSTRAINT IF EXISTS fk_agent_suggestions_resource,
    DROP CONSTRAINT IF EXISTS ck_agent_suggestions_type,
    DROP CONSTRAINT IF EXISTS ck_agent_suggestions_status;

ALTER TABLE agent_suggestions
    ADD CONSTRAINT ck_agent_suggestions_type
        CHECK (suggestion_type IN ('WBS', 'TASK', 'REQUIREMENT', 'SCHEDULE', 'REVIEW_ITEM', 'QUESTION', 'DOCUMENT_DRAFT')),
    ADD CONSTRAINT ck_agent_suggestions_status
        CHECK (status IN ('DRAFT', 'APPROVED', 'HELD', 'REJECTED')),
    ADD CONSTRAINT fk_agent_suggestions_job
        FOREIGN KEY (job_id) REFERENCES agent_jobs (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_agent_suggestions_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_agent_suggestions_user_status
    ON agent_suggestions (user_id, status);
CREATE INDEX IF NOT EXISTS idx_agent_suggestions_room_status
    ON agent_suggestions (room_id, status);
CREATE INDEX IF NOT EXISTS idx_agent_suggestions_job
    ON agent_suggestions (job_id);
CREATE INDEX IF NOT EXISTS idx_agent_suggestions_resource
    ON agent_suggestions (resource_id);
