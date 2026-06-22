package com.bubli.chat.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 채팅 메시지 원본.
 *
 * 테이블: chat_messages
 * 주요 필드: chat_room_id, sender_user_id, sender_guest_session_id,
 *           client_message_id, room_sequence, message_type, body
 *
 * message_type: USER_TEXT / AGENT_RESPONSE / SYSTEM
 * sender_user_id와 sender_guest_session_id 중 하나만 존재해야 한다 (CHECK 제약).
 * client_message_id로 중복 저장을 방지한다.
 * room_sequence로 채팅방 메시지 순서를 관리한다.
 * DB 저장 성공 후 WebSocket으로 전송한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
}
