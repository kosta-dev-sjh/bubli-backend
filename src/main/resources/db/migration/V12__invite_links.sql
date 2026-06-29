CREATE TABLE invite_links (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    token VARCHAR(100) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_invite_links_token ON invite_links (token);
