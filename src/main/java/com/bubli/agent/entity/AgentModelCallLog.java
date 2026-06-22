package com.bubli.agent.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * LLM 모델 호출 로그.
 *
 * 테이블: agent_model_call_logs
 * 주요 필드: agent_job_id, model_id, input_tokens, output_tokens,
 *           cost, duration_ms, created_at
 *
 * 사용자별 하루 모델 호출 제한 관리에 사용.
 * 비용 통제: 호출 로그 저장, 캐시 활용, 같은 hash 반복 분석 방지.
 * 로그에 모델 API key, 문서 원문, 에이전트 원문 대화는 저장하지 않는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentModelCallLog {
}
