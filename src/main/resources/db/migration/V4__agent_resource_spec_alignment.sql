CREATE TABLE IF NOT EXISTS resources (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    room_id UUID,
    title VARCHAR(255) NOT NULL,
    kind VARCHAR(20) NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_resources_kind
        CHECK (kind IN ('FILE', 'MEMO')),
    CONSTRAINT ck_resources_visibility
        CHECK (visibility IN ('PERSONAL', 'ROOM_SHARED')),
    CONSTRAINT ck_resources_status
        CHECK (status IN ('UPLOADING', 'READY', 'ANALYZING', 'ANALYZED', 'FAILED')),
    CONSTRAINT ck_resources_room_shared
        CHECK (visibility <> 'ROOM_SHARED' OR room_id IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_resources_owner_visibility
    ON resources (owner_id, visibility);
CREATE INDEX IF NOT EXISTS idx_resources_room_status
    ON resources (room_id, status);

CREATE TABLE IF NOT EXISTS resource_files (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    storage_key VARCHAR(1000) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_resource_files_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE CASCADE,
    CONSTRAINT ck_resource_files_size
        CHECK (size_bytes >= 0)
);

CREATE INDEX IF NOT EXISTS idx_resource_files_resource
    ON resource_files (resource_id);
CREATE INDEX IF NOT EXISTS idx_resource_files_checksum
    ON resource_files (checksum);

ALTER TABLE resource_files
    ADD COLUMN IF NOT EXISTS storage_key VARCHAR(1000);
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'resource_files'
          AND column_name = 'storage_path'
    ) THEN
        EXECUTE 'UPDATE resource_files SET storage_key = storage_path WHERE storage_key IS NULL';
    END IF;
END $$;
ALTER TABLE resource_files
    ALTER COLUMN storage_key SET NOT NULL;
ALTER TABLE resource_files
    DROP COLUMN IF EXISTS storage_path;
CREATE UNIQUE INDEX IF NOT EXISTS uk_resource_files_storage_key
    ON resource_files (storage_key);

CREATE TABLE IF NOT EXISTS resource_versions (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    version_no INTEGER NOT NULL,
    file_id UUID NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_resource_versions_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE CASCADE,
    CONSTRAINT fk_resource_versions_file
        FOREIGN KEY (file_id) REFERENCES resource_files (id) ON DELETE CASCADE,
    CONSTRAINT uk_resource_versions_resource_version
        UNIQUE (resource_id, version_no),
    CONSTRAINT ck_resource_versions_version
        CHECK (version_no >= 1)
);

CREATE INDEX IF NOT EXISTS idx_resource_versions_resource
    ON resource_versions (resource_id);
CREATE INDEX IF NOT EXISTS idx_resource_versions_file
    ON resource_versions (file_id);

CREATE TABLE IF NOT EXISTS resource_summaries (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    job_id UUID,
    status VARCHAR(20) NOT NULL,
    summary_json JSONB,
    checklist_json JSONB,
    prompt_version VARCHAR(50),
    schema_version VARCHAR(50),
    model_name VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_resource_summaries_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE CASCADE,
    CONSTRAINT ck_resource_summaries_status
        CHECK (status IN ('READY', 'ANALYZING', 'ANALYZED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_resource_summaries_resource
    ON resource_summaries (resource_id);
CREATE INDEX IF NOT EXISTS idx_resource_summaries_job
    ON resource_summaries (job_id);

ALTER TABLE resource_summaries
    ADD COLUMN IF NOT EXISTS job_id UUID,
    ADD COLUMN IF NOT EXISTS checklist_json JSONB,
    ADD COLUMN IF NOT EXISTS prompt_version VARCHAR(50),
    ADD COLUMN IF NOT EXISTS schema_version VARCHAR(50),
    ADD COLUMN IF NOT EXISTS model_name VARCHAR(100);

CREATE TABLE IF NOT EXISTS ai_documents (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    room_id UUID,
    document_type VARCHAR(30) NOT NULL,
    detected_confidence NUMERIC(5,4),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_ai_documents_resource
        UNIQUE (resource_id),
    CONSTRAINT fk_ai_documents_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE CASCADE,
    CONSTRAINT ck_ai_documents_type
        CHECK (document_type IN ('CONTRACT', 'REQUIREMENT', 'MEETING_NOTE', 'REFERENCE', 'QUOTATION', 'GENERAL')),
    CONSTRAINT ck_ai_documents_status
        CHECK (status IN ('READY', 'ANALYZING', 'ANALYZED', 'FAILED')),
    CONSTRAINT ck_ai_documents_confidence
        CHECK (detected_confidence IS NULL OR (detected_confidence >= 0 AND detected_confidence <= 1))
);

CREATE INDEX IF NOT EXISTS idx_ai_documents_room_status
    ON ai_documents (room_id, status);

CREATE TABLE IF NOT EXISTS agent_jobs (
    id UUID PRIMARY KEY,
    requested_by_user_id UUID NOT NULL,
    room_id UUID,
    resource_id UUID,
    job_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_code VARCHAR(100),
    error_message VARCHAR(1000),
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_agent_jobs_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE SET NULL,
    CONSTRAINT ck_agent_jobs_type
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
    CONSTRAINT ck_agent_jobs_status
        CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELED')),
    CONSTRAINT ck_agent_jobs_retry_count
        CHECK (retry_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_agent_jobs_room_status
    ON agent_jobs (room_id, status);
CREATE INDEX IF NOT EXISTS idx_agent_jobs_resource
    ON agent_jobs (resource_id);
CREATE INDEX IF NOT EXISTS idx_agent_jobs_requested_by
    ON agent_jobs (requested_by_user_id, created_at DESC);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'resource_summaries'
          AND constraint_name = 'fk_resource_summaries_job'
    ) THEN
        ALTER TABLE resource_summaries
            ADD CONSTRAINT fk_resource_summaries_job
                FOREIGN KEY (job_id) REFERENCES agent_jobs (id) ON DELETE SET NULL;
    END IF;
END $$;

ALTER TABLE agent_suggestions
    DROP CONSTRAINT IF EXISTS fk_agent_suggestions_request;
ALTER TABLE agent_suggestions
    DROP CONSTRAINT IF EXISTS ck_agent_suggestions_type;
ALTER TABLE agent_suggestions
    ADD CONSTRAINT ck_agent_suggestions_type
        CHECK (suggestion_type IN ('WBS', 'TASK', 'REQUIREMENT', 'SCHEDULE', 'REVIEW_ITEM', 'QUESTION', 'DOCUMENT_DRAFT'));

ALTER TABLE agent_suggestions
    DROP CONSTRAINT IF EXISTS ck_agent_suggestions_status;
ALTER TABLE agent_suggestions
    ADD CONSTRAINT ck_agent_suggestions_status
        CHECK (status IN ('DRAFT', 'APPROVED', 'HELD', 'REJECTED'));
ALTER TABLE agent_suggestions
    DROP CONSTRAINT IF EXISTS ck_agent_suggestions_confidence;

DROP INDEX IF EXISTS idx_agent_suggestions_request;
DROP INDEX IF EXISTS idx_agent_suggestions_source_document;

ALTER TABLE agent_suggestions
    ADD COLUMN IF NOT EXISTS user_id UUID,
    ADD COLUMN IF NOT EXISTS room_id UUID,
    ADD COLUMN IF NOT EXISTS job_id UUID,
    ADD COLUMN IF NOT EXISTS resource_id UUID,
    ADD COLUMN IF NOT EXISTS payload_json JSONB,
    ADD COLUMN IF NOT EXISTS evidence_json JSONB;

UPDATE agent_suggestions
SET room_id = COALESCE(room_id, project_room_id),
    resource_id = COALESCE(resource_id, source_document_id),
    job_id = COALESCE(job_id, agent_request_id),
    payload_json = COALESCE(payload_json, content_json),
    evidence_json = COALESCE(evidence_json, original_content_json)
WHERE room_id IS NULL
   OR resource_id IS NULL
   OR job_id IS NULL
   OR payload_json IS NULL
   OR evidence_json IS NULL;

ALTER TABLE agent_suggestions
    ALTER COLUMN user_id SET NOT NULL,
    ALTER COLUMN payload_json SET NOT NULL,
    ALTER COLUMN agent_request_id DROP NOT NULL,
    ALTER COLUMN project_room_id DROP NOT NULL,
    ALTER COLUMN title DROP NOT NULL,
    ALTER COLUMN original_content_json DROP NOT NULL,
    ALTER COLUMN content_json DROP NOT NULL;

ALTER TABLE agent_suggestions
    DROP COLUMN IF EXISTS agent_request_id,
    DROP COLUMN IF EXISTS project_room_id,
    DROP COLUMN IF EXISTS source_document_id,
    DROP COLUMN IF EXISTS title,
    DROP COLUMN IF EXISTS original_content_json,
    DROP COLUMN IF EXISTS content_json,
    DROP COLUMN IF EXISTS confidence,
    DROP COLUMN IF EXISTS row_version;

CREATE INDEX IF NOT EXISTS idx_agent_suggestions_user_status
    ON agent_suggestions (user_id, status);
CREATE INDEX IF NOT EXISTS idx_agent_suggestions_room_status
    ON agent_suggestions (room_id, status);
CREATE INDEX IF NOT EXISTS idx_agent_suggestions_job
    ON agent_suggestions (job_id);
CREATE INDEX IF NOT EXISTS idx_agent_suggestions_resource
    ON agent_suggestions (resource_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'agent_suggestions'
          AND constraint_name = 'fk_agent_suggestions_job'
    ) THEN
        ALTER TABLE agent_suggestions
            ADD CONSTRAINT fk_agent_suggestions_job
                FOREIGN KEY (job_id) REFERENCES agent_jobs (id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'agent_suggestions'
          AND constraint_name = 'fk_agent_suggestions_resource'
    ) THEN
        ALTER TABLE agent_suggestions
            ADD CONSTRAINT fk_agent_suggestions_resource
                FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE SET NULL;
    END IF;
END $$;
