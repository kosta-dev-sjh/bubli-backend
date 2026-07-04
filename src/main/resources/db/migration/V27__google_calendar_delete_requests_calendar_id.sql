ALTER TABLE google_calendar_delete_requests
    ADD COLUMN IF NOT EXISTS google_calendar_id VARCHAR(255);

UPDATE google_calendar_delete_requests
SET google_calendar_id = 'primary'
WHERE google_calendar_id IS NULL;

ALTER TABLE google_calendar_delete_requests
    ALTER COLUMN google_calendar_id SET NOT NULL;

ALTER TABLE google_calendar_delete_requests
    DROP CONSTRAINT IF EXISTS uk_google_calendar_delete_requests_user_event;

ALTER TABLE google_calendar_delete_requests
    ADD CONSTRAINT uk_google_calendar_delete_requests_user_calendar_event
        UNIQUE (user_id, google_calendar_id, google_event_id);
