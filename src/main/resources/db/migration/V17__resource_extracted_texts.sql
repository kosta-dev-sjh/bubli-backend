CREATE TABLE resource_extracted_texts (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    local_file_id VARCHAR(120) NOT NULL,
    checksum VARCHAR(128),
    extraction_method VARCHAR(80) NOT NULL,
    source_char_count INTEGER NOT NULL,
    analyzed_char_count INTEGER NOT NULL,
    combined_text TEXT NOT NULL,
    sentences_json JSONB NOT NULL,
    text_truncated BOOLEAN NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_resource_extracted_texts_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE CASCADE
);

CREATE INDEX idx_resource_extracted_texts_resource
    ON resource_extracted_texts (resource_id, updated_at DESC);

CREATE UNIQUE INDEX uk_resource_extracted_texts_resource_checksum_method
    ON resource_extracted_texts (resource_id, checksum, extraction_method)
    WHERE checksum IS NOT NULL;

CREATE UNIQUE INDEX uk_resource_extracted_texts_resource_local_file_method
    ON resource_extracted_texts (resource_id, local_file_id, extraction_method);
