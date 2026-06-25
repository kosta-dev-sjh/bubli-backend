CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY,
    resource_id UUID,
    project_room_id UUID,
    owner_id UUID NOT NULL,
    version_group_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    document_version INTEGER NOT NULL,
    is_latest BOOLEAN NOT NULL DEFAULT TRUE,
    error_message VARCHAR(1000),
    uploaded_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_documents_version_group_version
        UNIQUE (version_group_id, document_version),
    CONSTRAINT ck_documents_version_positive
        CHECK (document_version >= 1),
    CONSTRAINT ck_documents_project_scope
        CHECK (scope <> 'PROJECT' OR project_room_id IS NOT NULL),
    CONSTRAINT ck_documents_file_type
        CHECK (file_type IN ('PDF', 'TXT')),
    CONSTRAINT ck_documents_type
        CHECK (document_type IN ('CONTRACT', 'QUOTATION', 'REQUIREMENT', 'MEETING_NOTE', 'GENERAL')),
    CONSTRAINT ck_documents_scope
        CHECK (scope IN ('PROJECT', 'PERSONAL')),
    CONSTRAINT ck_documents_status
        CHECK (status IN ('UPLOADED', 'EXTRACTING', 'INDEXING', 'READY', 'FAILED', 'DELETED'))
);

CREATE INDEX IF NOT EXISTS idx_documents_room_status
    ON documents (project_room_id, status);
CREATE INDEX IF NOT EXISTS idx_documents_owner_scope
    ON documents (owner_id, scope);
CREATE INDEX IF NOT EXISTS idx_documents_checksum
    ON documents (checksum);
CREATE INDEX IF NOT EXISTS idx_documents_latest_version
    ON documents (version_group_id, is_latest);

CREATE UNIQUE INDEX IF NOT EXISTS uk_documents_one_latest_version
    ON documents (version_group_id)
    WHERE is_latest = TRUE;

CREATE TABLE IF NOT EXISTS document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    page_number INTEGER,
    section_title VARCHAR(500),
    token_count INTEGER NOT NULL,
    vector_store_id UUID,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_document_chunks_document
        FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT uk_document_chunks_document_index
        UNIQUE (document_id, chunk_index),
    CONSTRAINT ck_document_chunks_index
        CHECK (chunk_index >= 0),
    CONSTRAINT ck_document_chunks_page
        CHECK (page_number IS NULL OR page_number >= 1),
    CONSTRAINT ck_document_chunks_token_count
        CHECK (token_count >= 1)
);

CREATE INDEX IF NOT EXISTS idx_document_chunks_document_active
    ON document_chunks (document_id, active);
CREATE INDEX IF NOT EXISTS idx_document_chunks_vector_store_id
    ON document_chunks (vector_store_id);

CREATE TABLE IF NOT EXISTS agent_requests (
    id UUID PRIMARY KEY,
    project_room_id UUID NOT NULL,
    source_document_id UUID,
    request_user_id UUID NOT NULL,
    request_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    request_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    request_fingerprint VARCHAR(64) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    error_message VARCHAR(1000),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_agent_requests_type
        CHECK (request_type IN ('PROJECT_INIT', 'CONTRACT_CHECK', 'REQUIREMENT_ANALYSIS', 'WORK_CANDIDATE', 'DOCUMENT_DRAFT')),
    CONSTRAINT ck_agent_requests_status
        CHECK (status IN ('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_agent_requests_retry_count
        CHECK (retry_count >= 0 AND max_retries >= 0)
);

CREATE INDEX IF NOT EXISTS idx_agent_requests_room_status
    ON agent_requests (project_room_id, status);
CREATE INDEX IF NOT EXISTS idx_agent_requests_user_created
    ON agent_requests (request_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_agent_requests_fingerprint
    ON agent_requests (request_fingerprint);
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_requests_active_fingerprint
    ON agent_requests (request_fingerprint)
    WHERE status IN ('QUEUED', 'PROCESSING');

CREATE TABLE IF NOT EXISTS agent_suggestions (
    id UUID PRIMARY KEY,
    agent_request_id UUID NOT NULL,
    project_room_id UUID NOT NULL,
    source_document_id UUID,
    suggestion_type VARCHAR(40) NOT NULL,
    title VARCHAR(500) NOT NULL,
    original_content_json JSONB NOT NULL,
    content_json JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    confidence NUMERIC(5,4),
    reviewed_by UUID,
    reviewed_at TIMESTAMPTZ,
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_agent_suggestions_request
        FOREIGN KEY (agent_request_id) REFERENCES agent_requests (id) ON DELETE CASCADE,
    CONSTRAINT ck_agent_suggestions_type
        CHECK (suggestion_type IN ('PROJECT_INFO', 'CONTRACT_CHECK', 'REQUIREMENT', 'WBS', 'TODO', 'CONFIRMATION_QUESTION', 'DOCUMENT_DRAFT')),
    CONSTRAINT ck_agent_suggestions_status
        CHECK (status IN ('PENDING', 'APPROVED', 'MODIFIED', 'ON_HOLD', 'REJECTED')),
    CONSTRAINT ck_agent_suggestions_confidence
        CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1))
);

CREATE INDEX IF NOT EXISTS idx_agent_suggestions_room_status
    ON agent_suggestions (project_room_id, status);
CREATE INDEX IF NOT EXISTS idx_agent_suggestions_request
    ON agent_suggestions (agent_request_id);
CREATE INDEX IF NOT EXISTS idx_agent_suggestions_source_document
    ON agent_suggestions (source_document_id);

CREATE TABLE IF NOT EXISTS suggestion_evidences (
    id UUID PRIMARY KEY,
    suggestion_id UUID NOT NULL,
    document_id UUID NOT NULL,
    chunk_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    page_number INTEGER,
    evidence_text TEXT NOT NULL,
    similarity_score NUMERIC(8,7) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_suggestion_evidences_suggestion
        FOREIGN KEY (suggestion_id) REFERENCES agent_suggestions (id) ON DELETE CASCADE,
    CONSTRAINT fk_suggestion_evidences_document
        FOREIGN KEY (document_id) REFERENCES documents (id),
    CONSTRAINT fk_suggestion_evidences_chunk
        FOREIGN KEY (chunk_id) REFERENCES document_chunks (id),
    CONSTRAINT uk_suggestion_evidences_suggestion_chunk
        UNIQUE (suggestion_id, chunk_id),
    CONSTRAINT ck_suggestion_evidences_page
        CHECK (page_number IS NULL OR page_number >= 1),
    CONSTRAINT ck_suggestion_evidences_score
        CHECK (similarity_score >= 0 AND similarity_score <= 1)
);

CREATE INDEX IF NOT EXISTS idx_suggestion_evidences_document
    ON suggestion_evidences (document_id);
CREATE INDEX IF NOT EXISTS idx_suggestion_evidences_chunk
    ON suggestion_evidences (chunk_id);
