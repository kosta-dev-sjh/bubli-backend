/**
 * [voice] 보이스챗 도메인.
 *
 * 책임:
 * - LiveKit room 생성 (livekit_room_name 관리)
 * - LiveKit 참가 토큰 발급 (key/secret은 서버 전용)
 * - 참가자 상태 저장 (joined_at, left_at)
 * - 통화 종료 처리
 *
 * 엔티티:
 * - VoiceRoom        : 보이스챗 방 (room_id, chat_room_id, livekit_room_name, status: OPEN/ENDED)
 * - VoiceParticipant : 참가 기록 (voice_room_id, user_id, joined_at, left_at)
 *
 * 참가 권한: room_members 또는 chat_room_members
 * 초기: 프로젝트룸 보이스 우선. 1:1 보이스는 확장 후보.
 * 녹음, 음성 요약은 향후 확장으로 둔다.
 */
package com.bubli.voice;
