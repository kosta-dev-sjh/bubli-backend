package com.bubli.chat.service;

import com.bubli.chat.dto.ChatTypingEvent;
import com.bubli.user.dto.UserResult;
import com.bubli.user.service.UserPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatTypingService {

	private static final String CHAT_TOPIC_PREFIX = "/topic/chat/";
	private static final String TYPING_TOPIC_SUFFIX = "/typing";

	private final ChatRoomAccessPublicService chatRoomAccessPublicService;
	private final UserPublicService userPublicService;
	private final SimpMessagingTemplate messagingTemplate;

	@Transactional(readOnly = true)
	public ChatTypingEvent relayTyping(UUID userId, UUID chatRoomId, boolean typing) {
		chatRoomAccessPublicService.assertActiveMember(userId, chatRoomId);
		UserResult user = userPublicService.getUser(userId);
		ChatTypingEvent event = new ChatTypingEvent(chatRoomId, typing, user.id(), user.name());
		messagingTemplate.convertAndSend(CHAT_TOPIC_PREFIX + chatRoomId + TYPING_TOPIC_SUFFIX, event);
		return event;
	}
}
