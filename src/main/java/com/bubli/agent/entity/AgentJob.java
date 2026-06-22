package com.bubli.agent.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 에이전트 작업.
 *
 * 테이블: agent_jobs
 * 주요 필드: room_id, resource_id, job_type, status, requested_by, created_at
 *
 * job_type: 문서 분석, 계약 검토, 요구사항 추출, WBS 생성, TODO 생성,
 *           확인 질문 생성, 문서 초안 생성, 하루정리 등
 * status: PENDING → RUNNING → COMPLETED / FAILED
 *
 * 에이전트 분석 실패가 전체 서비스 장애로 이어지면 안 된다. (실패 상태 + 재시도 버튼)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
}
