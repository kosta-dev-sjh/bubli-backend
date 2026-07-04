package com.bubli.chat.service;

import com.bubli.chat.dto.ChatTypingEvent;
import com.bubli.user.dto.UserResult;
import com.bubli.user.service.UserPublicService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatTypingServiceTest {

	private final ChatRoomAccessPublicService chatRoomAccessPublicService = mock(ChatRoomAccessPublicService.class);
	private final UserPublicService userPublicService = mock(UserPublicService.class);
	private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
	private final ChatTypingService service = new ChatTypingService(
			chatRoomAccessPublicService,
			userPublicService,
			messagingTemplate
	);

	@Test
	void relayTypingPublishesAuthenticatedMemberTypingEvent() {
		UUID userId = UUID.randomUUID();
		UUID chatRoomId = UUID.randomUUID();
		when(userPublicService.getUser(userId))
				.thenReturn(new UserResult(userId, "maren", "마렌", null, "ko", "Asia/Seoul"));

		ChatTypingEvent event = service.relayTyping(userId, chatRoomId, true);

		assertThat(event).isEqualTo(new ChatTypingEvent(chatRoomId, true, userId, "마렌"));
		verify(chatRoomAccessPublicService).assertActiveMember(userId, chatRoomId);
		verify(messagingTemplate).convertAndSend("/topic/chat/" + chatRoomId + "/typing", event);
	}
}
