CREATE INDEX IF NOT EXISTS idx_voice_participants_room_status
    ON voice_participants (voice_room_id, status)
    INCLUDE (user_id, joined_at, left_at);

CREATE INDEX IF NOT EXISTS idx_voice_participants_room_user_created_at
    ON voice_participants (voice_room_id, user_id, created_at DESC);
