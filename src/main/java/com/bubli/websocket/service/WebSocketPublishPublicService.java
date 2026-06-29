package com.bubli.websocket.service;

import com.bubli.chat.dto.ChatMessageResult;

import java.time.Instant;
import java.util.UUID;

public interface WebSocketPublishPublicService {

	void publishChatMessage(ChatMessageResult message);

	void publishProjectRoomEvent(
			UUID eventId,
			String eventType,
			UUID roomId,
			Long sequence,
			Instant occurredAt,
			UUID actorUserId,
			String payloadJson
	);
}
