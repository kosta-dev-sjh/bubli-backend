package com.bubli.voice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 보이스챗 참가 기록.
 *
 * 테이블: voice_participants
 * 주요 필드: voice_room_id, user_id, guest_session_id, joined_at, left_at
 *
 * user_id와 guest_session_id 중 하나만 존재해야 한다 (CHECK 제약).
 * 회원은 room_members 권한, 게스트는 ACTIVE guest_session + chat_guest_access로 확인.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoiceParticipant {
}
