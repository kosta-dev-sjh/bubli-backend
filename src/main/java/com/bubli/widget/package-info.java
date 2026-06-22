/**
 * [widget] 위젯 버블 도메인.
 *
 * 책임:
 * - 버블 항목 상태 관리 (widget_item_states)
 * - 위젯 날짜별 사용 집계 관리 (widget_daily_summaries)
 * - 각 버블에 필요한 summary 데이터 조합
 * - 접근 가능한 프로젝트룸 정보 요약
 * - 타이머 표시 기준 관리
 *
 * 엔티티:
 * - WidgetItemState    : 버블별 항목 활성 상태, 사용자 기준 설정
 *                        (user_id, bubble_type, item_id, display_state)
 * - WidgetDailySummary : 날짜별 위젯 사용 집계
 *                        (user_id, summary_date, bubble_type, usage_json)
 *
 * 버블 종류: TODO, 에이전트, 소통, 타이머, 메모, 일정/WBS, 자료 제안, 알림
 *
 * 금지:
 * - TODO/일정/메모/타이머 원본 데이터를 widget 테이블에 복사 저장
 * - 상세 사용 이벤트 원문을 서버 DB에 저장 (Tauri SQLite에만 보관)
 * - 접근 권한 없는 프로젝트룸 데이터를 summary에 포함
 */
package com.bubli.widget;
