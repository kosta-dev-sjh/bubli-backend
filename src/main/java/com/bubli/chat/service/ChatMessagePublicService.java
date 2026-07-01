package com.bubli.chat.service;

import com.bubli.chat.dto.ChatMessageContextResult;
import com.bubli.chat.dto.ChatMessageResult;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

public interface ChatMessagePublicService {

	List<ChatMessageContextResult> getRecentRoomMessages(UUID userId, UUID roomId, int limit);

	ChatMessageResult createRoomAgentResponse(UUID userId, UUID roomId, JsonNode body, UUID resourceId);
}
