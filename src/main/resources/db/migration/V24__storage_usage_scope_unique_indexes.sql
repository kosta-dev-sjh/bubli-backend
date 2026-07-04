WITH ranked_personal_usage AS (
    SELECT
        id,
        user_id,
        (SUM(used_bytes) OVER (PARTITION BY user_id))::BIGINT AS merged_used_bytes,
        MAX(limit_bytes) OVER (PARTITION BY user_id) AS merged_limit_bytes,
        MAX(updated_at) OVER (PARTITION BY user_id) AS merged_updated_at,
        ROW_NUMBER() OVER (
            PARTITION BY user_id
            ORDER BY created_at, id
        ) AS row_number
    FROM storage_usage
    WHERE storage_scope = 'PERSONAL'
      AND user_id IS NOT NULL
)
UPDATE storage_usage usage
SET used_bytes = ranked.merged_used_bytes,
    limit_bytes = ranked.merged_limit_bytes,
    updated_at = ranked.merged_updated_at
FROM ranked_personal_usage ranked
WHERE usage.id = ranked.id
  AND ranked.row_number = 1;

WITH ranked_personal_usage AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY user_id
            ORDER BY created_at, id
        ) AS row_number
    FROM storage_usage
    WHERE storage_scope = 'PERSONAL'
      AND user_id IS NOT NULL
)
DELETE FROM storage_usage usage
USING ranked_personal_usage ranked
WHERE usage.id = ranked.id
  AND ranked.row_number > 1;

WITH ranked_room_usage AS (
    SELECT
        id,
        room_id,
        (SUM(used_bytes) OVER (PARTITION BY room_id))::BIGINT AS merged_used_bytes,
        MAX(limit_bytes) OVER (PARTITION BY room_id) AS merged_limit_bytes,
        MAX(updated_at) OVER (PARTITION BY room_id) AS merged_updated_at,
        ROW_NUMBER() OVER (
            PARTITION BY room_id
            ORDER BY created_at, id
        ) AS row_number
    FROM storage_usage
    WHERE storage_scope = 'ROOM'
      AND room_id IS NOT NULL
)
UPDATE storage_usage usage
SET used_bytes = ranked.merged_used_bytes,
    limit_bytes = ranked.merged_limit_bytes,
    updated_at = ranked.merged_updated_at
FROM ranked_room_usage ranked
WHERE usage.id = ranked.id
  AND ranked.row_number = 1;

WITH ranked_room_usage AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY room_id
            ORDER BY created_at, id
        ) AS row_number
    FROM storage_usage
    WHERE storage_scope = 'ROOM'
      AND room_id IS NOT NULL
)
DELETE FROM storage_usage usage
USING ranked_room_usage ranked
WHERE usage.id = ranked.id
  AND ranked.row_number > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_storage_usage_personal_user
    ON storage_usage (user_id)
    WHERE storage_scope = 'PERSONAL'
      AND user_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_storage_usage_room
    ON storage_usage (room_id)
    WHERE storage_scope = 'ROOM'
      AND room_id IS NOT NULL;
