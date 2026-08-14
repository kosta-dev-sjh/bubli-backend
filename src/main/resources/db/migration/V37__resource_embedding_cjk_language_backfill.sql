-- Pure Han-script Japanese chunks were previously classified as unknown because V35 only checked kana.
-- Bubli currently supports Korean, English, and Japanese document search, so Han text without Hangul is Japanese.
UPDATE resource_embeddings
SET chunk_metadata = jsonb_set(
    COALESCE(chunk_metadata, '{}'::jsonb),
    '{documentLanguage}',
    '"ja"'::jsonb,
    true
)
WHERE COALESCE(chunk_metadata ->> 'documentLanguage', 'unknown') = 'unknown'
  AND chunk_text ~ '[㐀-䶿一-鿿豈-﫿]'
  AND chunk_text !~ '[가-힣]';
