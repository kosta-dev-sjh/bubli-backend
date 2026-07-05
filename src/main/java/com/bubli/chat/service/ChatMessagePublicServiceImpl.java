package com.bubli.chat.service;

import com.bubli.chat.dto.ChatMessageContextResult;
import com.bubli.chat.dto.ChatMessageResult;
import com.bubli.chat.entity.ChatMessage;
import com.bubli.chat.entity.ChatRoom;
import com.bubli.chat.entity.ChatRoomMember;
import com.bubli.chat.repository.ChatMessageRepository;
import com.bubli.chat.repository.ChatRoomMemberRepository;
import com.bubli.chat.repository.ChatRoomRepository;
import com.bubli.chat.type.ChatMemberStatus;
import com.bubli.chat.type.ChatType;
import com.bubli.chat.type.MessageType;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.websocket.service.WebSocketPublishPublicService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessagePublicServiceImpl implements ChatMessagePublicService {

	private static final int MESSAGE_SEQUENCE_SAVE_MAX_ATTEMPTS = 3;

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
		ensureActiveChatRoomMember(chatRoom.getId(), userId);
		ChatMessage message = createAgentResponseMessageWithRetry(chatRoom.getId(), userId, body, resourceId);
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

	private ChatMessage createAgentResponseMessageWithRetry(UUID chatRoomId, UUID userId, JsonNode body,
			UUID resourceId) {
		DataIntegrityViolationException lastException = null;
		for (int attempt = 0; attempt < MESSAGE_SEQUENCE_SAVE_MAX_ATTEMPTS; attempt++) {
			try {
				long nextSequence = chatMessageRepository.findMaxRoomSequence(chatRoomId) + 1;
				return chatMessageRepository.saveAndFlush(ChatMessage.create(
						chatRoomId,
						userId,
						"agent-response-" + UUID.randomUUID(),
						nextSequence,
						MessageType.AGENT_RESPONSE,
						writeBody(body),
						resourceId
				));
			} catch (DataIntegrityViolationException exception) {
				lastException = exception;
			}
		}
		if (lastException == null) {
			throw new IllegalStateException("Message save retry attempts must be positive.");
		}
		throw lastException;
	}

	private void ensureActiveChatRoomMember(UUID chatRoomId, UUID userId) {
		chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
				.ifPresentOrElse(
						member -> {
							if (member.getStatus() != ChatMemberStatus.ACTIVE) {
								member.reactivate();
							}
						},
						() -> chatRoomMemberRepository.save(ChatRoomMember.create(chatRoomId, userId))
				);
	}

	private String writeBody(JsonNode body) {
		try {
			return objectMapper.writeValueAsString(body);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
