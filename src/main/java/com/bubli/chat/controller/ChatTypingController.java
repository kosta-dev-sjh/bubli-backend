package com.bubli.chat.controller;

import com.bubli.chat.dto.ChatTypingRequest;
import com.bubli.chat.service.ChatTypingService;
import com.bubli.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatTypingController {

	private final ChatTypingService chatTypingService;

	@MessageMapping("/chat/{chatRoomId}/typing")
	public void relayTyping(
			@DestinationVariable UUID chatRoomId,
			@Valid @Payload ChatTypingRequest request,
			Principal principal
	) {
		chatTypingService.relayTyping(authUser(principal).userId(), chatRoomId, request.typing());
	}

	private AuthUser authUser(Principal principal) {
		if (principal instanceof Authentication authentication
				&& authentication.getPrincipal() instanceof AuthUser authUser) {
			return authUser;
		}
		throw new MessagingException("WebSocket typing event requires authentication.");
	}
}
