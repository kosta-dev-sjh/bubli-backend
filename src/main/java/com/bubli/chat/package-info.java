/**
 * [chat] 채팅 도메인.
 *
 * 책임:
 * - 1:1 채팅방 (friendship 기준, DIRECT 타입)
 * - 프로젝트룸 채팅방 (room_members 기준, ROOM 타입)
 * - 메시지 저장 (chat_messages가 원본)
 * - client_message_id 중복 방지
 * - room_sequence 발급 (메시지 순서)
 * - WebSocket/STOMP 전달 (/topic/chat/{chatRoomId})
 * - 프로젝트룸 에이전트 명령어 수신 → agent 공개 Service 호출
 *   (/bubli 정리, /bubli todo, /bubli 질문)
 * - 에이전트 응답을 AGENT_RESPONSE 메시지로 저장
 *
 * 엔티티:
 * - ChatRoom        : 채팅방 (room_id, chat_type: ROOM/DIRECT)
 * - ChatRoomMember  : 채팅방 참여자와 읽은 위치
 * - ChatMessage     : 메시지 (chat_room_id, sender_user_id, client_message_id,
 *                     room_sequence, message_type, body)
 *
 * 1:1 채팅은 에이전트를 호출하지 않는다.
 * 개인 에이전트 대화는 서버 DB에 저장하지 않는다.
 */
package com.bubli.chat;
