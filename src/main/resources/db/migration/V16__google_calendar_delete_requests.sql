CREATE TABLE google_calendar_delete_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    google_event_id VARCHAR(255) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_google_calendar_delete_requests_user_event UNIQUE (user_id, google_event_id),
    CONSTRAINT fk_google_calendar_delete_requests_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_google_calendar_delete_requests_user
    ON google_calendar_delete_requests (user_id);
