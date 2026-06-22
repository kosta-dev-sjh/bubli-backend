package com.bubli.voice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 보이스챗 방.
 *
 * 테이블: voice_rooms
 * 주요 필드: room_id(프로젝트룸), chat_room_id, livekit_room_name(UNIQUE), status
 *
 * status: OPEN / ENDED
 * LiveKit roomName과 Bubli 프로젝트룸을 연결한다.
 * LiveKit API key/secret은 서버에서만 관리한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoiceRoom {
}
