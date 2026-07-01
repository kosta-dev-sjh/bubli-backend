package com.bubli.websocket.service;

import com.bubli.chat.dto.ChatMessageResponse;
import com.bubli.chat.dto.ChatMessageResult;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.personal.notification.dto.NotificationResponse;
import com.bubli.project.dto.ProjectRoomEventActorResponse;
import com.bubli.project.dto.ProjectRoomEventResponse;
import com.bubli.user.dto.UserResult;
import com.bubli.user.service.UserPublicService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebSocketPublishPublicServiceImpl implements WebSocketPublishPublicService {

	private static final String CHAT_TOPIC_PREFIX = "/topic/chat/";
	private static final String PROJECT_ROOM_EVENTS_TOPIC_PREFIX = "/topic/project-rooms/";
	private static final String PROJECT_ROOM_EVENTS_TOPIC_SUFFIX = "/events";
	private static final String USER_NOTIFICATION_QUEUE = "/queue/notifications";
	private static final String UNKNOWN_USER_NAME = "Unknown";

	private final SimpMessagingTemplate messagingTemplate;
	private final UserPublicService userPublicService;
	private final ObjectMapper objectMapper;

	@Override
	public void publishChatMessage(ChatMessageResult message) {
		publishAfterCommit(CHAT_TOPIC_PREFIX + message.chatRoomId(), ChatMessageResponse.from(message));
	}

	@Override
	public void publishProjectRoomEvent(
			UUID eventId,
			String eventType,
			UUID roomId,
			Long sequence,
			Instant occurredAt,
			UUID actorUserId,
			String payloadJson
	) {
		publishAfterCommit(
				PROJECT_ROOM_EVENTS_TOPIC_PREFIX + roomId + PROJECT_ROOM_EVENTS_TOPIC_SUFFIX,
				new ProjectRoomEventResponse(
						eventId,
						eventType,
						roomId,
						sequence,
						occurredAt,
						actor(actorUserId),
						payload(payloadJson)
				)
		);
	}

	@Override
	public void publishUserNotification(UUID userId, NotificationResponse notification) {
		publishToUserAfterCommit(userId.toString(), USER_NOTIFICATION_QUEUE, notification);
	}

	private void publishAfterCommit(String destination, Object payload) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			messagingTemplate.convertAndSend(destination, payload);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				messagingTemplate.convertAndSend(destination, payload);
			}
		});
	}

	private void publishToUserAfterCommit(String user, String destination, Object payload) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			messagingTemplate.convertAndSendToUser(user, destination, payload);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				messagingTemplate.convertAndSendToUser(user, destination, payload);
			}
		});
	}

	private ProjectRoomEventActorResponse actor(UUID actorUserId) {
		if (actorUserId == null) {
			return ProjectRoomEventActorResponse.system();
		}
		try {
			UserResult user = userPublicService.getUser(actorUserId);
			return ProjectRoomEventActorResponse.user(user.id(), user.name());
		} catch (BusinessException exception) {
			return ProjectRoomEventActorResponse.user(actorUserId, UNKNOWN_USER_NAME);
		}
	}

	private JsonNode payload(String payloadJson) {
		try {
			return objectMapper.readTree(payloadJson);
		} catch (JsonProcessingException exception) {
			throw new BusinessException(ErrorCode.COMMON_500_001);
		}
	}
}
