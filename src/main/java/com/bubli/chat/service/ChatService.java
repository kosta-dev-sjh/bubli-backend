package com.bubli.chat.service;

import com.bubli.chat.dto.ChatMessageResult;
import com.bubli.chat.dto.ChatRoomReadResponse;
import com.bubli.chat.dto.ChatRoomResult;
import com.bubli.chat.dto.SendChatMessageCommand;
import com.bubli.chat.entity.ChatMessage;
import com.bubli.chat.entity.ChatRoom;
import com.bubli.chat.entity.ChatRoomMember;
import com.bubli.chat.repository.ChatMessageRepository;
import com.bubli.chat.repository.ChatRoomMemberRepository;
import com.bubli.chat.repository.ChatRoomRepository;
import com.bubli.chat.type.ChatMemberStatus;
import com.bubli.chat.type.ChatType;
import com.bubli.chat.type.MessageType;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.user.dto.UserResult;
import com.bubli.user.service.UserPublicService;
import com.bubli.websocket.service.WebSocketPublishPublicService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

	private static final String USER_SENDER_TYPE = "USER";
	private static final String UNKNOWN_USER_NAME = "알 수 없음";

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final UserPublicService userPublicService;
	private final ObjectMapper objectMapper;
	private final WebSocketPublishPublicService webSocketPublishPublicService;

	@Transactional(readOnly = true)
	public PageResponse<ChatRoomResult> getChatRooms(UUID userId, Pageable pageable) {
		Page<ChatRoomResult> page = chatRoomRepository
				.findAccessibleRooms(userId, ChatMemberStatus.ACTIVE, pageable)
				.map(ChatRoomResult::from);

		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext()
		);
	}

	@Transactional
	public ChatRoomResult createDirectRoom(UUID requesterId, UUID targetUserId) {
		if (requesterId.equals(targetUserId)) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}

		UserResult targetUser = userPublicService.getUser(targetUserId);

		return chatRoomRepository.findDirectRoomBetween(
						requesterId,
						targetUserId,
						ChatType.DIRECT,
						ChatMemberStatus.ACTIVE
				)
				.map(ChatRoomResult::from)
				.orElseGet(() -> createNewDirectRoom(requesterId, targetUser));
	}

	@Transactional(readOnly = true)
	public PageResponse<ChatMessageResult> getMessages(UUID userId, UUID chatRoomId,
			Long beforeSequence, Long afterSequence, Pageable pageable) {
		checkActiveMember(userId, chatRoomId);

		Page<ChatMessage> page;
		if (afterSequence != null) {
			page = chatMessageRepository.findByChatRoomIdAndRoomSequenceGreaterThanOrderByRoomSequenceAsc(
					chatRoomId,
					afterSequence,
					pageable
			);
		} else if (beforeSequence != null) {
			page = chatMessageRepository.findByChatRoomIdAndRoomSequenceLessThanOrderByRoomSequenceDesc(
					chatRoomId,
					beforeSequence,
					pageable
			);
		} else {
			page = chatMessageRepository.findByChatRoomIdOrderByRoomSequenceDesc(chatRoomId, pageable);
		}

		Map<UUID, UserResult> senders = userPublicService.getUsers(page.map(ChatMessage::getSenderUserId));

		return new PageResponse<>(
				page.getContent().stream()
						.map(message -> toResult(message, senders.get(message.getSenderUserId())))
						.toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext()
		);
	}

	@Transactional
	public ChatMessageResult sendMessage(UUID senderUserId, UUID chatRoomId, SendChatMessageCommand command) {
		checkActiveMember(senderUserId, chatRoomId);

		boolean newMessage = false;
		ChatMessage message = chatMessageRepository
				.findByChatRoomIdAndClientMessageId(chatRoomId, command.clientMessageId())
				.orElse(null);
		if (message == null) {
			message = createMessage(senderUserId, chatRoomId, command);
			newMessage = true;
		}

		UserResult sender = userPublicService.getUser(senderUserId);
		ChatMessageResult result = toResult(message, sender);
		if (newMessage) {
			webSocketPublishPublicService.publishChatMessage(result);
		}
		return result;
	}

	@Transactional
	public ChatRoomReadResponse markRead(UUID userId, UUID chatRoomId, Long lastReadSequence) {
		ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserIdAndStatus(
				chatRoomId,
				userId,
				ChatMemberStatus.ACTIVE
		).orElseThrow(() -> new BusinessException(ErrorCode.CHAT_403_001));

		ChatMessage message = chatMessageRepository.findByChatRoomIdAndRoomSequence(chatRoomId, lastReadSequence)
				.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_404_002));

		Instant now = Instant.now();
		member.markRead(message.getId(), message.getRoomSequence(), now);
		return new ChatRoomReadResponse(chatRoomId, message.getRoomSequence(), now);
	}

	private ChatMessage createMessage(UUID senderUserId, UUID chatRoomId, SendChatMessageCommand command) {
		ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_404_001));

		long nextSequence = chatMessageRepository.findMaxRoomSequence(chatRoom.getId()) + 1;
		ChatMessage message = ChatMessage.create(
				chatRoom.getId(),
				senderUserId,
				command.clientMessageId(),
				nextSequence,
				command.messageType() == null ? MessageType.TEXT : command.messageType(),
				writeBody(command.body()),
				command.resourceId()
		);
		return chatMessageRepository.save(message);
	}

	private ChatRoomResult createNewDirectRoom(UUID requesterId, UserResult targetUser) {
		ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.createDirect(targetUser.name()));
		chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom.getId(), requesterId));
		chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom.getId(), targetUser.id()));
		return ChatRoomResult.from(chatRoom);
	}

	private void checkActiveMember(UUID userId, UUID chatRoomId) {
		boolean activeMember = chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndStatus(
				chatRoomId,
				userId,
				ChatMemberStatus.ACTIVE
		);
		if (!activeMember) {
			throw new BusinessException(ErrorCode.CHAT_403_001);
		}
	}

	private ChatMessageResult toResult(ChatMessage message, UserResult sender) {
		return new ChatMessageResult(
				message.getId(),
				message.getChatRoomId(),
				USER_SENDER_TYPE,
				message.getSenderUserId(),
				sender == null ? UNKNOWN_USER_NAME : sender.name(),
				message.getClientMessageId(),
				message.getRoomSequence(),
				message.getMessageType(),
				readBody(message.getBody()),
				message.getResourceId(),
				message.getCreatedAt()
		);
	}

	private String writeBody(JsonNode body) {
		try {
			return objectMapper.writeValueAsString(body);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private JsonNode readBody(String body) {
		try {
			return objectMapper.readTree(body);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException(exception);
		}
	}

}
