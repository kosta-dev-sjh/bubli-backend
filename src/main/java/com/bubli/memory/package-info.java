/**
 * [memory] 메모리/요약 도메인.
 *
 * 책임:
 * - 프로젝트룸 장기기억 관리 (room_memory_summaries)
 *   → 결정사항, 남은 질문, TODO/WBS 후보, 관련 자료
 *   → from_sequence, to_sequence로 요약 범위 기록
 * - 사용자 하루정리 요약 관리 (daily_summaries)
 *   → 사용자가 확인한 개인 하루정리 요약만 서버 저장
 *   → 원문 대화는 Tauri SQLite에만 보관
 *
 * 엔티티:
 * - RoomMemorySummary : 프로젝트룸 채팅/작업 맥락 장기요약
 *                       (room_id, from_sequence, to_sequence, summary_json, status)
 * - DailySummary      : 개인 하루정리 요약
 *                       (user_id, summary_date, summary_json, status: DRAFT/APPROVED)
 *
 * 개인 에이전트 원문 대화는 memory에 저장하지 않는다.
 * 에이전트가 만든 요약 후보를 그대로 확정하지 않는다.
 */
package com.bubli.memory;
