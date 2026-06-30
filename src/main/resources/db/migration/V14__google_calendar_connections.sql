CREATE TABLE google_calendar_connections (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    google_account_email VARCHAR(255),
    access_token TEXT NOT NULL,
    refresh_token TEXT,
    expires_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_google_calendar_connections_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT ck_google_calendar_connections_status CHECK (status IN ('ACTIVE', 'REVOKED'))
);

CREATE INDEX idx_google_calendar_connections_user_status
    ON google_calendar_connections (user_id, status);
