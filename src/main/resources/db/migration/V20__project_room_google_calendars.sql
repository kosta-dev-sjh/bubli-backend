CREATE TABLE project_room_google_calendars (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    room_id UUID NOT NULL,
    google_calendar_id VARCHAR(255) NOT NULL,
    calendar_name VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_project_room_google_calendars_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_project_room_google_calendars_room FOREIGN KEY (room_id) REFERENCES project_rooms(id),
    CONSTRAINT uk_project_room_google_calendars_user_room UNIQUE (user_id, room_id)
);

CREATE INDEX idx_project_room_google_calendars_room
    ON project_room_google_calendars (room_id);
