package com.bubli.project.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 프로젝트룸 초대.
 *
 * 테이블: invitations
 * 주요 필드: room_id, inviter_id, invitee_user_id(친구초대),
 *           invite_type(FRIEND/LINK), token_hash(링크초대),
 *           accepted_by_user_id(링크 수락자), role, status, expires_at
 *
 * 상태: PENDING → ACCEPTED / EXPIRED / CANCELED
 * 친구 초대: 같은 room+invitee에 PENDING이 있으면 새 초대를 만들지 않는다.
 * 링크 초대: 로그인 사용자만 수락 가능. 기본 만료 7일.
 * 수락 시: invitations.status=ACCEPTED, room_members에 ACTIVE 생성.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation {
}
