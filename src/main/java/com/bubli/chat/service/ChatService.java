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
import com.bubli.chat.type.MessageType;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

	private static final String USER_SENDER_TYPE = "USER";
	private static final String UNKNOWN_USER_NAME = "알 수 없음";

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;

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

		Map<UUID, User> senders = findUsersById(page.map(ChatMessage::getSenderUserId));

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

		ChatMessage message = chatMessageRepository
				.findByChatRoomIdAndClientMessageId(chatRoomId, command.clientMessageId())
				.orElseGet(() -> createMessage(senderUserId, chatRoomId, command));

		User sender = userRepository.findById(senderUserId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_404_001));
		return toResult(message, sender);
	}

	@Transactional
	public ChatRoomReadResponse markRead(UUID userId, UUID chatRoomId, UUID lastReadMessageId) {
		ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserIdAndStatus(
				chatRoomId,
				userId,
				ChatMemberStatus.ACTIVE
		).orElseThrow(() -> new BusinessException(ErrorCode.CHAT_403_001));

		ChatMessage message = chatMessageRepository.findByIdAndChatRoomId(lastReadMessageId, chatRoomId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_404_002));

		Instant now = Instant.now();
		member.markRead(message.getId(), now);
		return new ChatRoomReadResponse(chatRoomId, message.getId(), now);
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

	private ChatMessageResult toResult(ChatMessage message, User sender) {
		return new ChatMessageResult(
				message.getId(),
				message.getChatRoomId(),
				USER_SENDER_TYPE,
				message.getSenderUserId(),
				sender == null ? UNKNOWN_USER_NAME : sender.getName(),
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

	private Map<UUID, User> findUsersById(Page<UUID> userIds) {
		return userRepository.findAllById(userIds.getContent()).stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));
	}
}
