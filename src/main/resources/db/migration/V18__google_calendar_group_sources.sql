ALTER TABLE schedules
    ADD COLUMN IF NOT EXISTS google_calendar_id VARCHAR(255);

ALTER TABLE schedules
    ADD COLUMN IF NOT EXISTS google_calendar_summary VARCHAR(255);

UPDATE schedules
SET google_calendar_id = 'primary'
WHERE google_event_id IS NOT NULL
  AND google_calendar_id IS NULL;

ALTER TABLE schedules DROP CONSTRAINT IF EXISTS schedules_google_event_id_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_schedules_owner_google_calendar_event
    ON schedules (owner_user_id, google_calendar_id, google_event_id)
    WHERE google_event_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_schedules_owner_google_calendar_starts
    ON schedules (owner_user_id, google_calendar_id, starts_at)
    WHERE google_calendar_id IS NOT NULL;
