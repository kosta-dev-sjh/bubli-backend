package com.bubli.memory.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자가 확인한 개인 하루정리 요약.
 *
 * 테이블: daily_summaries
 * 주요 필드: user_id, summary_date, summary_json, source_range_json, status
 *
 * status: DRAFT / APPROVED
 *
 * 에이전트가 하루정리 요약 후보를 만들면 사용자가 확인한 뒤 저장.
 * 원문 대화가 아니라 요약만 서버에 저장한다.
 * 개인 에이전트 원문 대화는 Tauri SQLite에만 보관.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailySummary {
}
