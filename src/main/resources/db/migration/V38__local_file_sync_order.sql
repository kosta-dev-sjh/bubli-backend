ALTER TABLE resources ADD COLUMN local_sync_device_id VARCHAR(120);
ALTER TABLE resources ADD COLUMN local_sync_sequence BIGINT NOT NULL DEFAULT 0;
ALTER TABLE resources ADD CONSTRAINT ck_resources_local_sync_sequence
CHECK (local_sync_sequence >= 0 AND
       ((local_sync_sequence = 0 AND local_sync_device_id IS NULL) OR
        (local_sync_sequence > 0 AND local_sync_device_id IS NOT NULL)));
