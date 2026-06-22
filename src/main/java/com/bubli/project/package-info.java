/**
 * [project] 프로젝트 및 프로젝트룸 도메인.
 *
 * 책임:
 * - 프로젝트 CRUD (생성 시 기본 프로젝트룸 1개 자동 생성)
 * - 프로젝트룸 CRUD
 * - room_members 관리 (PROJECT_LEADER/MEMBER, ACTIVE/LEFT/REMOVED)
 * - 친구 초대 (FRIEND 타입, invitee_user_id, 중복 PENDING 방지)
 * - 링크 초대 (LINK 타입, token_hash, expires_at, 로그인 사용자만 수락)
 * - 게스트 세션 (비회원 임시 참여, 채팅+보이스만, 기본 2시간 만료)
 * - 멤버 역할 변경, 나가기, 내보내기
 * - 프로젝트 리더 0명 방지
 * - 계약 참고값 (estimate_amount, contract_amount, payment_status 등)
 *
 * 엔티티:
 * - Project      : 프리랜서가 맡은 프로젝트 (owner_id, name, client_name, 참고값)
 * - ProjectRoom  : 프로젝트 안의 업무 공간 (project_id, status: ACTIVE/ARCHIVED)
 * - RoomMember   : 프로젝트룸 참여자 (room_id, user_id, role, status)
 * - Invitation   : 초대 (FRIEND/LINK 타입, token_hash, status, expires_at)
 * - GuestSession : 비회원 게스트 세션 (display_name, token_hash, status, expires_at)
 *
 * 프로젝트룸 접근 권한의 기준은 room_members다.
 */
package com.bubli.project;
