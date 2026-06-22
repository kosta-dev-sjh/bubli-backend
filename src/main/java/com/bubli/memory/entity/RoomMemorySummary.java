package com.bubli.memory.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 프로젝트룸 채팅/작업 맥락의 장기요약.
 *
 * 테이블: room_memory_summaries
 * 주요 필드: room_id, from_sequence, to_sequence, summary_json, created_by, status
 *
 * 저장 내용: 결정사항, 남은 질문, TODO 후보, WBS 후보, 관련 자료,
 *           요약한 메시지 범위 (from_sequence ~ to_sequence)
 * status: DRAFT / APPROVED
 *
 * /bubli 명령어 실행 시 에이전트가 생성하고, 필요한 장기요약만 저장한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomMemorySummary {
}
