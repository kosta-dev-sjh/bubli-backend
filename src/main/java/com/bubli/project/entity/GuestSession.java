package com.bubli.project.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 비회원 게스트의 임시 참여 세션.
 *
 * 테이블: guest_sessions
 * 주요 필드: room_id, created_by_user_id(프로젝트 리더), display_name,
 *           token_hash, status(PENDING/ACTIVE/EXPIRED/REVOKED), expires_at
 *
 * 게스트는 room_members에 들어가지 않는다.
 * 게스트는 채팅과 보이스챗만 사용할 수 있다.
 * 자료, WBS/TODO, 일정, 다운로드에 접근할 수 없다.
 * 기본 만료: 2시간. 프로젝트 리더가 직접 종료 가능.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuestSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
}
