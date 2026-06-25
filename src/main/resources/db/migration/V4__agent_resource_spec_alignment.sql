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
    original_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
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
    status VARCHAR(20) NOT NULL,
    summary_json JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_resource_summaries_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE CASCADE,
    CONSTRAINT ck_resource_summaries_status
        CHECK (status IN ('READY', 'ANALYZING', 'ANALYZED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_resource_summaries_resource
    ON resource_summaries (resource_id);

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
