package com.bubli.chat.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 채팅방.
 *
 * 테이블: chat_rooms
 * 주요 필드: room_id(프로젝트룸 채팅일 때), chat_type(PROJECT_ROOM/DIRECT), title
 *
 * PROJECT_ROOM: 프로젝트룸 멤버 간 채팅. room_members 권한 기준.
 * DIRECT: 친구 간 1:1 채팅. friendships 기준.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
}
