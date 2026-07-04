CREATE TABLE local_file_sync_events (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    local_event_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    resource_id UUID,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_local_file_sync_events_user_event UNIQUE (user_id, local_event_id)
);

CREATE INDEX idx_local_file_sync_events_user_created
    ON local_file_sync_events(user_id, created_at DESC);
