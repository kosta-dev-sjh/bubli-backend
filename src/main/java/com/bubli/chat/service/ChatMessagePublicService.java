package com.bubli.chat.service;

import com.bubli.chat.dto.ChatMessageContextResult;

import java.util.List;
import java.util.UUID;

public interface ChatMessagePublicService {

	List<ChatMessageContextResult> getRecentRoomMessages(UUID userId, UUID roomId, int limit);
}
