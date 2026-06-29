/**
 * [project] 프로젝트룸 도메인.
 *
 * 책임:
 * - 프로젝트와 프로젝트룸을 합친 project_rooms CRUD
 * - room_members 관리 (PROJECT_LEADER/MEMBER, ACTIVE/LEFT/REMOVED)
 * - 기존 회원 ID 기준 프로젝트룸 초대 (invitee_user_id, 중복 PENDING 방지)
 * - 게스트 초대, 링크 초대 제외
 * - 멤버 역할 변경, 나가기, 내보내기
 * - 프로젝트 리더 0명 방지
 * - 계약 참고값 (contract_amount, payment_status 등)
 *
 * 엔티티:
 * - ProjectRoom      : 프로젝트룸 원본 (created_by_user_id, name, client_name, status: ACTIVE/CLOSED)
 * - RoomMember   : 프로젝트룸 참여자 (room_id, user_id, role, status)
 * - Invitation   : 기존 회원 초대 (invitee_user_id, status, expires_at)
 * - ProjectRoomEvent : 프로젝트룸 갱신 이벤트 (room_id, sequence, event_type, payload_json)
 *
 * 프로젝트룸 접근 권한의 기준은 room_members다.
 */
package com.bubli.project;
