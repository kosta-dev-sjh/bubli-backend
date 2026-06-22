package com.bubli.chat.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 게스트의 채팅방 접근 허용.
 *
 * 테이블: chat_guest_access
 * 주요 필드: chat_room_id, guest_session_id, can_send_message, can_join_voice
 *
 * 게스트가 접근 가능한 프로젝트룸 채팅방과 허용 기능 범위를 관리한다.
 * ACTIVE guest_session에서만 접근 허용.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatGuestAccess {
}
