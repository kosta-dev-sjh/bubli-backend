package com.bubli.project.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 프리랜서가 맡은 프로젝트.
 *
 * 테이블: projects
 * 주요 필드: owner_id, name, client_name, status, starts_at, ends_at
 * 참고값: estimate_amount, contract_amount, contract_status, payment_status, payment_due_at, paid_at
 *
 * 프로젝트 생성 시 기본 프로젝트룸 1개를 자동 생성한다.
 * 사용자당 프로젝트 수: 무료 기준 5개.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
}
