WITH ranked_root_wbs_items AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY room_id
            ORDER BY order_no, created_at, id
        ) AS normalized_order_no
    FROM wbs_items
    WHERE parent_id IS NULL
)
UPDATE wbs_items item
SET order_no = ranked.normalized_order_no
FROM ranked_root_wbs_items ranked
WHERE item.id = ranked.id
  AND item.order_no <> ranked.normalized_order_no;

CREATE UNIQUE INDEX IF NOT EXISTS uk_wbs_items_room_root_order
    ON wbs_items (room_id, order_no)
    WHERE parent_id IS NULL;
