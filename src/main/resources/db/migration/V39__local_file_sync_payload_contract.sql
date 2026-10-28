ALTER TABLE local_file_sync_events ADD COLUMN payload_fingerprint VARCHAR(80);
ALTER TABLE local_file_sync_events ADD COLUMN error_code VARCHAR(80);
ALTER TABLE local_file_sync_events ADD COLUMN retryable BOOLEAN NOT NULL DEFAULT FALSE;

-- Old rows deliberately remain unknown: their original request cannot be reconstructed.
UPDATE local_file_sync_events SET retryable = TRUE WHERE status IN ('PROCESSING', 'FAILED');
