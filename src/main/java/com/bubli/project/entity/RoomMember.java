package com.bubli.project.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 프로젝트룸 참여자.
 *
 * 테이블: room_members
 * 주요 필드: room_id, user_id, role(PROJECT_LEADER/MEMBER),
 *           status(ACTIVE/LEFT/REMOVED), joined_at, left_at
 *
 * 프로젝트룸 자료, 채팅, WBS, 보이스 접근 권한의 기준.
 * 활성 프로젝트룸에는 최소 1명의 PROJECT_LEADER가 있어야 한다.
 * 마지막 리더는 나가기 전에 다른 멤버에게 권한을 넘겨야 한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
}
