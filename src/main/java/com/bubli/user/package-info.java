/**
 * [user] 사용자 도메인.
 *
 * 책임:
 * - 내 프로필 조회/수정 (이름, 아바타, 언어, 시간대)
 * - 사용자별 설정 (테마, 밀도, 글자 크기, 기본 프로젝트룸)
 * - 알림 설정 (메시지/댓글/자료버전/에이전트/용량초과 On/Off)
 * - 개인정보 동의 (활동 감지, 로컬 폴더 접근 동의)
 * - 친구 관리 (Bubli ID 검색, 요청/수락/거절/차단)
 *
 * 주요 엔티티:
 * - User                       : 회원 (google_sub, bubli_id, name)
 * - UserPreference             : 테마, 밀도, 기본 프로젝트룸
 * - UserNotificationPreference : 알림 종류별 On/Off
 * - UserPrivacyConsent         : 활동 감지, 폴더 접근 동의
 * - FriendRequest              : 친구 요청 (PENDING/ACCEPTED/REJECTED/CANCELED)
 * - Friendship                 : 수락된 친구 관계 (ACCEPTED/BLOCKED)
 *
 * 사용자별 설정은 프로젝트룸 설정과 분리한다.
 */
package com.bubli.user;
