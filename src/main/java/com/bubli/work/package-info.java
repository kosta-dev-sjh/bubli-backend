/**
 * [work] 업무 관리 도메인 그룹.
 * TODO, WBS, 내부 일정 원본을 책임진다.
 *
 * task     - 개인 TODO, 프로젝트룸 TODO CRUD, 상태 변경
 *            (TODO/IN_PROGRESS/REVIEW/DONE/BLOCKED)
 *            에이전트 후보 승인 → 실제 tasks 생성
 *            → 엔티티: Task (owner_user_id, assignee_user_id, room_id, status, due_at)
 *            에이전트 후보는 agent.agent_suggestions가 소유하고, 승인 후 tasks로 확정한다.
 * wbs      - WBS CRUD, 줄형 작업 구조, 순서 변경, 후보 → 확정 반영
 *            → 엔티티: WbsItem (room_id, parent_id, title, order_no, status)
 *            → 화면: WBS 트리, 칸반, 타임라인, 간트
 * schedule - 구글 캘린더 연동 일정, task/wbs_item/room 연결 일정
 *            마감일 기반 조회, 대시보드/위젯 일정 데이터 제공
 *            → 엔티티: Schedule (owner_user_id, room_id, task_id, wbs_item_id, starts_at, ends_at)
 *
 * work는 하위 도메인을 묶는 상위 패키지다.
 * work 바로 아래에 controller/service/repository/entity/type을 만들지 않는다.
 * 개인 TODO와 프로젝트룸 TODO는 같은 tasks 원본을 사용하고 owner_user_id, room_id, assignee_user_id로 구분한다.
 */
package com.bubli.work;
