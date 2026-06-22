package com.bubli.project.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 프로젝트 안의 업무 공간(프로젝트룸).
 *
 * 테이블: project_rooms
 * 주요 필드: project_id, owner_id, name, status(ACTIVE/ARCHIVED), archived_at
 *
 * 혼자 사용 가능하고, 친구를 초대하면 같은 자료/채팅/WBS/TODO를 공유한다.
 * 접근 권한의 기준은 room_members다.
 * 기본 활성 기간: 6개월. 만료 후 읽기 전용 보관.
 * 멤버 수 기준: 5명 정도.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectRoom {
}
