package com.bubli.chat.service;

import com.bubli.chat.dto.ChatMessageContextResult;
import com.bubli.chat.dto.ChatMessageResult;
import com.bubli.chat.entity.ChatMessage;
import com.bubli.chat.entity.ChatRoom;
import com.bubli.chat.entity.ChatRoomMember;
import com.bubli.chat.repository.ChatMessageRepository;
import com.bubli.chat.repository.ChatRoomMemberRepository;
import com.bubli.chat.repository.ChatRoomRepository;
import com.bubli.chat.type.ChatType;
import com.bubli.chat.type.MessageType;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.websocket.service.WebSocketPublishPublicService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessagePublicServiceImpl implements ChatMessagePublicService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ChatRoomAccessPublicService chatRoomAccessPublicService;
	private final ProjectMembershipPublicService projectMembershipPublicService;
	private final ObjectMapper objectMapper;
	private final WebSocketPublishPublicService webSocketPublishPublicService;

	@Override
	@Transactional(readOnly = true)
	public List<ChatMessageContextResult> getRecentRoomMessages(UUID userId, UUID roomId, int limit) {
		return chatRoomRepository.findByRoomIdAndChatType(roomId, ChatType.ROOM)
				.map(chatRoom -> {
					chatRoomAccessPublicService.assertActiveMember(userId, chatRoom.getId());
					return chatMessageRepository.findByChatRoomIdOrderByRoomSequenceDesc(
									chatRoom.getId(),
									PageRequest.of(0, Math.max(1, Math.min(limit, 20)))
							).stream()
							.map(ChatMessageContextResult::from)
							.sorted(Comparator.comparing(ChatMessageContextResult::roomSequence))
							.toList();
				})
				.orElseGet(List::of);
	}

	@Override
	@Transactional
	public ChatMessageResult createRoomAgentResponse(UUID userId, UUID roomId, JsonNode body, UUID resourceId) {
		projectMembershipPublicService.assertActiveMember(userId, roomId);
		ChatRoom chatRoom = chatRoomRepository.findByRoomIdAndChatType(roomId, ChatType.ROOM)
				.orElseGet(() -> chatRoomRepository.save(ChatRoom.createRoom(roomId, "프로젝트룸 채팅")));
		if (!chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndStatus(
				chatRoom.getId(),
				userId,
				com.bubli.chat.type.ChatMemberStatus.ACTIVE
		)) {
			chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom.getId(), userId));
		}
		long nextSequence = chatMessageRepository.findMaxRoomSequence(chatRoom.getId()) + 1;
		ChatMessage message = chatMessageRepository.save(ChatMessage.create(
				chatRoom.getId(),
				userId,
				"agent-response-" + UUID.randomUUID(),
				nextSequence,
				MessageType.AGENT_RESPONSE,
				writeBody(body),
				resourceId
		));
		ChatMessageResult result = new ChatMessageResult(
				message.getId(),
				message.getChatRoomId(),
				"AGENT",
				userId,
				"Bubli Agent",
				message.getClientMessageId(),
				message.getRoomSequence(),
				message.getMessageType(),
				body,
				message.getResourceId(),
				message.getCreatedAt()
		);
		webSocketPublishPublicService.publishChatMessage(result);
		return result;
	}

	private String writeBody(JsonNode body) {
		try {
			return objectMapper.writeValueAsString(body);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
