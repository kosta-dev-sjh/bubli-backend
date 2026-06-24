ALTER TABLE user_preferences
    ADD COLUMN IF NOT EXISTS default_home_type VARCHAR(30);

ALTER TABLE user_preferences
    ADD COLUMN IF NOT EXISTS default_project_room_id UUID;

UPDATE user_preferences
SET default_project_room_id = default_room_id
WHERE default_project_room_id IS NULL
  AND default_room_id IS NOT NULL;

ALTER TABLE user_preferences
    DROP COLUMN IF EXISTS default_room_id;

ALTER TABLE user_preferences
    DROP COLUMN IF EXISTS font_scale;
