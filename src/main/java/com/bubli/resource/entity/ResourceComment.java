package com.bubli.resource.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 프로젝트룸 자료 댓글.
 *
 * 테이블: resource_comments
 * 주요 필드: resource_id, author_id, body, parent_id
 *
 * 프로젝트룸 자료에만 우선 적용한다.
 * 접근 권한: 해당 자료의 접근 권한과 동일 (room_members).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
}
