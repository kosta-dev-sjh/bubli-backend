ALTER TABLE chat_messages
    DROP CONSTRAINT IF EXISTS chat_messages_client_message_id_key;

ALTER TABLE chat_messages
    ADD CONSTRAINT uk_chat_messages_room_client_message UNIQUE (chat_room_id, client_message_id);

ALTER TABLE chat_room_members
    ADD COLUMN last_read_sequence BIGINT;
