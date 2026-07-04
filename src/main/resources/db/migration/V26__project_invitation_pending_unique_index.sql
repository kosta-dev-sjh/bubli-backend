WITH ranked_pending_invitations AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY room_id, invitee_user_id
            ORDER BY created_at ASC, id ASC
        ) AS rn
    FROM invitations
    WHERE status = 'PENDING'
)
UPDATE invitations
SET status = 'CANCELED',
    updated_at = now()
FROM ranked_pending_invitations ranked
WHERE invitations.id = ranked.id
  AND ranked.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_invitations_room_invitee_pending
    ON invitations (room_id, invitee_user_id)
    WHERE status = 'PENDING';
