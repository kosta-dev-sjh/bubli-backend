package com.bubli.resource.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 개인 자료 또는 프로젝트룸 자료 카드.
 *
 * 테이블: resources
 * 주요 필드: owner_id, project_id, room_id, title, kind, visibility, status
 *
 * visibility: PERSONAL(owner만 접근) / ROOM_SHARED(room_members 접근)
 * status: UPLOADING → READY → ANALYZING → ANALYZED / FAILED / DELETE_CANDIDATE / ARCHIVED
 *
 * PERSONAL 자료는 프로젝트룸 멤버에게 보이지 않으며,
 * 프로젝트룸 에이전트 context에 포함되지 않는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
}
