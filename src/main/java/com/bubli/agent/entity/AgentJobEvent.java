package com.bubli.agent.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 에이전트 작업 이벤트 로그.
 *
 * 테이블: agent_job_events
 * 주요 필드: agent_job_id, event_type, payload_json, created_at
 *
 * 작업의 시작, 진행, 완료, 실패 등 이벤트를 기록한다.
 * 에이전트 디버깅과 재시도 판단에 사용.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentJobEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
}
