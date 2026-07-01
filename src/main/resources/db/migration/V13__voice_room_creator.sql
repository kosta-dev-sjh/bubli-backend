ALTER TABLE voice_rooms ADD COLUMN IF NOT EXISTS created_by_user_id UUID;
ALTER TABLE voice_rooms ALTER COLUMN chat_room_id DROP NOT NULL;
