WITH duplicated_open_rooms AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY room_id
            ORDER BY created_at DESC, id DESC
        ) AS row_number
    FROM voice_rooms
    WHERE room_id IS NOT NULL
      AND status = 'OPEN'
)
UPDATE voice_rooms
SET status = 'ENDED'
WHERE id IN (
    SELECT id
    FROM duplicated_open_rooms
    WHERE row_number > 1
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_voice_rooms_room_open
    ON voice_rooms (room_id)
    WHERE room_id IS NOT NULL
      AND status = 'OPEN';
