/**
 * [work] 업무 관리 도메인 그룹.
 * TODO, WBS, 내부 일정 원본을 책임진다.
 *
 * task     - 개인 TODO, 프로젝트룸 TODO CRUD, 상태 변경
 *            (TODO/IN_PROGRESS/REVIEW/DONE/BLOCKED)
 *            에이전트 후보 승인 → 실제 tasks 생성
 *            → 엔티티: Task (owner_id, assignee_id, room_id, status, due_at)
 *            → 엔티티: TaskCandidate (에이전트가 생성한 TODO 후보, DRAFT/APPROVED/HELD/REJECTED)
 * wbs      - WBS CRUD, 트리 구조, 순서 변경, 후보 → 확정 반영
 *            → 엔티티: WbsItem (project_id, room_id, parent_id, title, order_no, status)
 *            → 화면: WBS 트리, 칸반, 타임라인, 간트
 * schedule - 내부 일정 CRUD, task/wbs_item/project 연결 일정
 *            마감일 기반 조회, 대시보드/위젯 일정 데이터 제공
 *            → 엔티티: Schedule (user_id, task_id, wbs_item_id, starts_at, ends_at)
 *
 * 개인 TODO와 프로젝트룸 TODO는 같은 tasks 원본을 사용하고
 * owner_id, room_id, assignee_id로 구분한다.
 */
package com.bubli.work;
