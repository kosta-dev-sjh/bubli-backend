-- Persist the detected source-document language on each searchable chunk.
-- Existing chunks receive a conservative script-based initial value; re-indexing refines it.
UPDATE resource_embeddings
SET chunk_metadata = jsonb_set(
    COALESCE(chunk_metadata, '{}'::jsonb),
    '{documentLanguage}',
    to_jsonb(
        CASE
            WHEN chunk_text ~ '[가-힣]' THEN 'ko'
            WHEN chunk_text ~ '[ぁ-んァ-ン]' THEN 'ja'
            WHEN chunk_text ~ '[A-Za-z]' THEN 'en'
            ELSE 'unknown'
        END
    ),
    true
)
WHERE COALESCE(chunk_metadata ->> 'documentLanguage', '') = '';

CREATE INDEX IF NOT EXISTS idx_resource_embeddings_document_language
    ON resource_embeddings ((chunk_metadata ->> 'documentLanguage'));
