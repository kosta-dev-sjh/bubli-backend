CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS resource_embeddings (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    room_id UUID,
    visibility VARCHAR(20) NOT NULL,
    chunk_index INTEGER NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding vector(1024) NOT NULL,
    chunk_metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_resource_embeddings_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE CASCADE,
    CONSTRAINT uk_resource_embeddings_resource_chunk
        UNIQUE (resource_id, chunk_index),
    CONSTRAINT ck_resource_embeddings_visibility
        CHECK (visibility IN ('PERSONAL', 'ROOM_SHARED')),
    CONSTRAINT ck_resource_embeddings_room_shared
        CHECK (visibility <> 'ROOM_SHARED' OR room_id IS NOT NULL),
    CONSTRAINT ck_resource_embeddings_chunk_index
        CHECK (chunk_index >= 0)
);

CREATE INDEX IF NOT EXISTS idx_resource_embeddings_resource
    ON resource_embeddings (resource_id);
CREATE INDEX IF NOT EXISTS idx_resource_embeddings_owner_visibility
    ON resource_embeddings (owner_id, visibility);
CREATE INDEX IF NOT EXISTS idx_resource_embeddings_room_visibility
    ON resource_embeddings (room_id, visibility);
CREATE INDEX IF NOT EXISTS idx_resource_embeddings_vector_hnsw
    ON resource_embeddings
    USING hnsw (embedding vector_cosine_ops);

DROP TABLE IF EXISTS suggestion_evidences;
DROP TABLE IF EXISTS agent_requests;
DROP TABLE IF EXISTS document_chunks;
DROP TABLE IF EXISTS documents;
