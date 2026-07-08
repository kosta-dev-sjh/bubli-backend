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
import com.bubli.personal.notification.service.NotificationPublicService;
import com.bubli.personal.notification.type.NotificationSourceType;
import com.bubli.project.dto.ProjectRoomResult;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.project.service.ProjectRoomPublicService;
import com.bubli.user.dto.UserResult;
import com.bubli.user.service.UserPublicService;
import com.bubli.websocket.service.WebSocketPublishPublicService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

	private static final String USER_SENDER_TYPE = "USER";
	private static final String UNKNOWN_USER_NAME = "알 수 없음";
	private static final int MESSAGE_SEQUENCE_SAVE_MAX_ATTEMPTS = 3;

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final UserPublicService userPublicService;
	private final ProjectRoomPublicService projectRoomPublicService;
	private final ProjectMembershipPublicService projectMembershipPublicService;
	private final ObjectMapper objectMapper;
	private final WebSocketPublishPublicService webSocketPublishPublicService;
	private final NotificationPublicService notificationPublicService;

	@Transactional(readOnly = true)
	public PageResponse<ChatRoomResult> getChatRooms(UUID userId, Pageable pageable) {
		Page<ChatRoom> roomPage = chatRoomRepository.findAccessibleRooms(userId, ChatMemberStatus.ACTIVE, pageable);
		Map<UUID, String> directRoomNames = resolveDirectRoomNames(userId, roomPage.getContent());

		Page<ChatRoomResult> page = roomPage.map(chatRoom ->
				ChatRoomResult.from(chatRoom, directRoomNames.getOrDefault(chatRoom.getId(), chatRoom.getName())));

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

		ChatRoom existingRoom = chatRoomRepository.findDirectRoomBetween(
				requesterId,
				targetUserId,
				ChatType.DIRECT,
				ChatMemberStatus.ACTIVE
		).orElse(null);
		if (existingRoom != null) {
			return ChatRoomResult.from(existingRoom, targetUser.name());
		}

		ChatRoom chatRoom = createNewDirectRoom(requesterId, targetUser);
		UserResult requester = userPublicService.getUser(requesterId);
		notificationPublicService.create(
				targetUserId,
				NotificationSourceType.CHAT_INVITE,
				chatRoom.getId(),
				requester.name(),
				"1:1 대화를 시작했습니다"
		);

		return ChatRoomResult.from(chatRoom, targetUser.name());
	}

	@Transactional
	public ChatRoomResult createGroupRoom(UUID creatorId, String name, List<UUID> memberUserIds) {
		Set<UUID> memberIds = normalizedMemberIds(creatorId, memberUserIds);
		if (name == null || name.isBlank() || memberIds.size() < 2) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}

		memberIds.stream()
				.filter(memberId -> !memberId.equals(creatorId))
				.forEach(userPublicService::getUser);

		ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.createGroup(name.trim()));
		memberIds.forEach(memberId -> chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom.getId(), memberId)));

		UserResult creator = userPublicService.getUser(creatorId);
		memberIds.stream()
				.filter(memberId -> !memberId.equals(creatorId))
				.forEach(memberId -> notificationPublicService.create(
						memberId,
						NotificationSourceType.CHAT_INVITE,
						chatRoom.getId(),
						creator.name(),
						name.trim() + " 그룹 채팅에 초대했습니다"
				));

		return ChatRoomResult.from(chatRoom);
	}

	@Transactional
	public ChatRoomResult createProjectRoomChatRoom(UUID requesterId, UUID roomId) {
		ProjectRoomResult projectRoom = projectRoomPublicService.getProjectRoom(requesterId, roomId);
		ChatRoom chatRoom = chatRoomRepository.findByRoomIdAndChatType(roomId, ChatType.ROOM)
				.orElseGet(() -> chatRoomRepository.save(ChatRoom.createRoom(roomId, projectRoom.name())));

		addActiveProjectMembers(chatRoom.getId(), roomId);
		return ChatRoomResult.from(chatRoom);
	}

	@Transactional
	public ChatRoomResult inviteMembers(UUID inviterId, UUID chatRoomId, List<UUID> memberUserIds) {
		checkActiveMember(inviterId, chatRoomId);
		ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_404_001));
		if (chatRoom.getChatType() != ChatType.GROUP) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}

		Set<UUID> memberIds = normalizedMemberIds(inviterId, memberUserIds);
		memberIds.remove(inviterId);
		if (memberIds.isEmpty()) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}

		memberIds.forEach(userPublicService::getUser);
		syncChatRoomMembers(chatRoomId, memberIds);

		UserResult inviter = userPublicService.getUser(inviterId);
		memberIds.forEach(memberId -> notificationPublicService.create(
				memberId,
				NotificationSourceType.CHAT_INVITE,
				chatRoomId,
				inviter.name(),
				chatRoom.getName() + " 그룹 채팅에 초대했습니다"
		));

		return ChatRoomResult.from(chatRoom);
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

		ChatMessage message = chatMessageRepository
				.findByChatRoomIdAndClientMessageId(chatRoomId, command.clientMessageId())
				.orElse(null);
		MessageSaveResult saveResult = message == null
				? createMessageWithRetry(senderUserId, chatRoomId, command)
				: new MessageSaveResult(message, false);

		UserResult sender = userPublicService.getUser(senderUserId);
		ChatMessageResult result = toResult(saveResult.message(), sender);
		if (saveResult.created()) {
			webSocketPublishPublicService.publishChatMessage(result);
			notifyRecipients(chatRoomId, senderUserId, sender, command);
		}
		return result;
	}

	private void notifyRecipients(UUID chatRoomId, UUID senderUserId, UserResult sender, SendChatMessageCommand command) {
		List<ChatRoomMember> recipients = chatRoomMemberRepository
				.findByChatRoomIdAndUserIdNotAndStatus(chatRoomId, senderUserId, ChatMemberStatus.ACTIVE);
		if (recipients.isEmpty()) {
			return;
		}

		String body = messagePreview(command);
		recipients.forEach(recipient -> notificationPublicService.create(
				recipient.getUserId(),
				NotificationSourceType.MESSAGE,
				chatRoomId,
				sender.name(),
				body
		));
	}

	private String messagePreview(SendChatMessageCommand command) {
		MessageType messageType = command.messageType() == null ? MessageType.TEXT : command.messageType();
		if (messageType == MessageType.TEXT && command.body() != null && command.body().hasNonNull("text")) {
			return command.body().get("text").asText();
		}
		return switch (messageType) {
			case FILE -> "파일을 보냈습니다.";
			case AGENT_COMMAND -> "에이전트 명령을 보냈습니다.";
			case AGENT_RESPONSE -> "에이전트 응답이 도착했습니다.";
			default -> "새 메시지가 도착했습니다.";
		};
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

	@Transactional
	public void leaveRoom(UUID userId, UUID chatRoomId) {
		ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_404_001));
		if (chatRoom.getChatType() == ChatType.ROOM) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}

		ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserIdAndStatus(
				chatRoomId,
				userId,
				ChatMemberStatus.ACTIVE
		).orElseThrow(() -> new BusinessException(ErrorCode.CHAT_403_001));

		member.leave();
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
		return chatMessageRepository.saveAndFlush(message);
	}

	private MessageSaveResult createMessageWithRetry(UUID senderUserId, UUID chatRoomId,
			SendChatMessageCommand command) {
		DataIntegrityViolationException lastException = null;
		for (int attempt = 0; attempt < MESSAGE_SEQUENCE_SAVE_MAX_ATTEMPTS; attempt++) {
			try {
				return new MessageSaveResult(createMessage(senderUserId, chatRoomId, command), true);
			} catch (DataIntegrityViolationException exception) {
				ChatMessage existingMessage = chatMessageRepository
						.findByChatRoomIdAndClientMessageId(chatRoomId, command.clientMessageId())
						.orElse(null);
				if (existingMessage != null) {
					return new MessageSaveResult(existingMessage, false);
				}
				lastException = exception;
			}
		}
		if (lastException == null) {
			throw new IllegalStateException("Message save retry attempts must be positive.");
		}
		throw lastException;
	}

	private ChatRoom createNewDirectRoom(UUID requesterId, UserResult targetUser) {
		ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.createDirect(targetUser.name()));
		chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom.getId(), requesterId));
		chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom.getId(), targetUser.id()));
		return chatRoom;
	}

	private Map<UUID, String> resolveDirectRoomNames(UUID viewerUserId, List<ChatRoom> rooms) {
		List<UUID> directRoomIds = rooms.stream()
				.filter(room -> room.getChatType() == ChatType.DIRECT)
				.map(ChatRoom::getId)
				.toList();
		if (directRoomIds.isEmpty()) {
			return Map.of();
		}

		Map<UUID, UUID> counterpartUserIdByRoomId = chatRoomMemberRepository
				.findByChatRoomIdInAndUserIdNot(directRoomIds, viewerUserId)
				.stream()
				.collect(Collectors.toMap(
						ChatRoomMember::getChatRoomId,
						ChatRoomMember::getUserId,
						(first, second) -> first
				));

		Map<UUID, UserResult> counterpartUsers = userPublicService.getUsers(counterpartUserIdByRoomId.values());

		Map<UUID, String> names = new HashMap<>();
		counterpartUserIdByRoomId.forEach((roomId, counterpartUserId) -> {
			UserResult counterpart = counterpartUsers.get(counterpartUserId);
			if (counterpart != null) {
				names.put(roomId, counterpart.name());
			}
		});
		return names;
	}

	private void addActiveProjectMembers(UUID chatRoomId, UUID roomId) {
		syncChatRoomMembers(chatRoomId, projectMembershipPublicService.findActiveMemberIds(roomId));
	}

	private void syncChatRoomMembers(UUID chatRoomId, Collection<UUID> userIds) {
		List<UUID> memberIds = userIds.stream()
				.filter(Objects::nonNull)
				.distinct()
				.toList();
		if (memberIds.isEmpty()) {
			return;
		}

		Map<UUID, ChatRoomMember> existingMemberByUserId = chatRoomMemberRepository
				.findByChatRoomIdAndUserIdIn(chatRoomId, memberIds)
				.stream()
				.collect(Collectors.toMap(
						ChatRoomMember::getUserId,
						member -> member,
						(first, second) -> first
				));

		memberIds.forEach(memberId -> {
			ChatRoomMember existingMember = existingMemberByUserId.get(memberId);
			if (existingMember == null) {
				chatRoomMemberRepository.save(ChatRoomMember.create(chatRoomId, memberId));
				return;
			}
			if (existingMember.getStatus() != ChatMemberStatus.ACTIVE) {
				existingMember.reactivate();
			}
		});
	}

	private Set<UUID> normalizedMemberIds(UUID requesterId, List<UUID> memberUserIds) {
		if (memberUserIds == null || memberUserIds.isEmpty()) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}

		Set<UUID> memberIds = new LinkedHashSet<>();
		memberIds.add(requesterId);
		memberUserIds.stream()
				.filter(memberId -> memberId != null && !memberId.equals(requesterId))
				.forEach(memberIds::add);
		return memberIds;
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

	private record MessageSaveResult(ChatMessage message, boolean created) {
	}

}
