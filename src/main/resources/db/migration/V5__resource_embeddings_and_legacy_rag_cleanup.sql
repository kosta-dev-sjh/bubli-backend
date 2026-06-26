-- Finalize ResourceEmbedding for the V1 baseline schema and remove legacy RAG tables.

CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE resource_embeddings
    ALTER COLUMN visibility TYPE VARCHAR(20);

ALTER TABLE resource_embeddings
    DROP CONSTRAINT IF EXISTS fk_resource_embeddings_resource,
    DROP CONSTRAINT IF EXISTS ck_resource_embeddings_visibility,
    DROP CONSTRAINT IF EXISTS ck_resource_embeddings_room_shared,
    DROP CONSTRAINT IF EXISTS ck_resource_embeddings_chunk_index;

ALTER TABLE resource_embeddings
    ADD CONSTRAINT fk_resource_embeddings_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE CASCADE,
    ADD CONSTRAINT ck_resource_embeddings_visibility
        CHECK (visibility IN ('PERSONAL', 'ROOM_SHARED')),
    ADD CONSTRAINT ck_resource_embeddings_room_shared
        CHECK (visibility <> 'ROOM_SHARED' OR room_id IS NOT NULL),
    ADD CONSTRAINT ck_resource_embeddings_chunk_index
        CHECK (chunk_index >= 0);

CREATE INDEX IF NOT EXISTS idx_resource_embeddings_resource
    ON resource_embeddings (resource_id);
CREATE INDEX IF NOT EXISTS idx_resource_embeddings_owner_visibility
    ON resource_embeddings (owner_id, visibility);
CREATE INDEX IF NOT EXISTS idx_resource_embeddings_room_visibility
    ON resource_embeddings (room_id, visibility);
CREATE INDEX IF NOT EXISTS idx_resource_embeddings_vector_hnsw
    ON resource_embeddings
    USING hnsw (embedding vector_cosine_ops);

DROP TABLE IF EXISTS suggestion_evidences CASCADE;
DROP TABLE IF EXISTS agent_requests CASCADE;
DROP TABLE IF EXISTS document_chunks CASCADE;
DROP TABLE IF EXISTS documents CASCADE;
