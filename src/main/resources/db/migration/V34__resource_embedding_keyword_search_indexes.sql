-- Improve AI keyword retrieval over resource embedding chunks.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_resource_embeddings_chunk_text_fts
    ON resource_embeddings
    USING gin (to_tsvector('simple', coalesce(chunk_text, '')));

CREATE INDEX IF NOT EXISTS idx_resource_embeddings_chunk_text_trgm
    ON resource_embeddings
    USING gin (lower(chunk_text) gin_trgm_ops);
