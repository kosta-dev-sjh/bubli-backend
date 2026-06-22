package com.bubli.agent.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 에이전트가 생성한 제안/후보.
 *
 * 테이블: agent_suggestions
 * 주요 필드: user_id, room_id, suggestion_type, payload_json, status
 *
 * suggestion_type: 요구사항 후보, WBS 후보, TODO 후보, 확인 질문, 문서 초안,
 *                  하루정리, 작업 메모, 버블 제안 등
 * status: DRAFT → APPROVED / HELD / REJECTED
 *
 * 사용자가 승인하기 전에는 확정 데이터(tasks, wbs_items 등)로 반영하지 않는다.
 * 승인 후 실제 데이터 반영은 대상 도메인 Service가 처리한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
}
