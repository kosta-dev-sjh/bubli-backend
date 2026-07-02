package com.bubli.chat.controller;

import com.bubli.chat.dto.ChatMessageResponse;
import com.bubli.chat.dto.ChatMessageResult;
import com.bubli.chat.dto.ChatRoomReadResponse;
import com.bubli.chat.dto.ChatRoomResponse;
import com.bubli.chat.dto.ChatRoomResult;
import com.bubli.chat.dto.CreateDirectChatRoomRequest;
import com.bubli.chat.dto.CreateGroupChatRoomRequest;
import com.bubli.chat.dto.MarkChatRoomReadRequest;
import com.bubli.chat.dto.SendChatMessageRequest;
import com.bubli.chat.service.ChatService;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.response.PageResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;

	@GetMapping("/api/chat/rooms")
	public ApiResponse<PageResponse<ChatRoomResponse>> getChatRooms(
			@CurrentUser AuthUser authUser,
			@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(mapRoomPage(chatService.getChatRooms(authUser.userId(), pageable)));
	}

	@PostMapping("/api/chat/direct-rooms")
	public ApiResponse<ChatRoomResponse> createDirectRoom(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody CreateDirectChatRoomRequest request
	) {
		return ApiResponse.success(ChatRoomResponse.from(
				chatService.createDirectRoom(authUser.userId(), request.targetUserId())
		));
	}

	@PostMapping("/api/chat/group-rooms")
	public ApiResponse<ChatRoomResponse> createGroupRoom(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody CreateGroupChatRoomRequest request
	) {
		return ApiResponse.success(ChatRoomResponse.from(
				chatService.createGroupRoom(authUser.userId(), request.name(), request.memberUserIds())
		));
	}

	@GetMapping("/api/chat/rooms/{chatRoomId}/messages")
	public ApiResponse<PageResponse<ChatMessageResponse>> getMessages(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID chatRoomId,
			@RequestParam(required = false) Long beforeSequence,
			@RequestParam(required = false) Long afterSequence,
			@PageableDefault(size = 30) Pageable pageable
	) {
		return ApiResponse.success(mapMessagePage(chatService.getMessages(
				authUser.userId(),
				chatRoomId,
				beforeSequence,
				afterSequence,
				pageable
		)));
	}

	@PostMapping("/api/chat/rooms/{chatRoomId}/messages")
	public ApiResponse<ChatMessageResponse> sendMessage(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID chatRoomId,
			@Valid @RequestBody SendChatMessageRequest request
	) {
		return ApiResponse.success(ChatMessageResponse.from(
				chatService.sendMessage(authUser.userId(), chatRoomId, request.toCommand())
		));
	}

	@PatchMapping("/api/chat/rooms/{chatRoomId}/read")
	public ApiResponse<ChatRoomReadResponse> markRead(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID chatRoomId,
			@Valid @RequestBody MarkChatRoomReadRequest request
	) {
		return ApiResponse.success(chatService.markRead(authUser.userId(), chatRoomId, request.lastReadSequence()));
	}

	private PageResponse<ChatRoomResponse> mapRoomPage(PageResponse<ChatRoomResult> page) {
		return new PageResponse<>(
				page.getItems().stream()
						.map(ChatRoomResponse::from)
						.toList(),
				page.getPage(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isHasNext()
		);
	}

	private PageResponse<ChatMessageResponse> mapMessagePage(PageResponse<ChatMessageResult> page) {
		return new PageResponse<>(
				page.getItems().stream()
						.map(ChatMessageResponse::from)
						.toList(),
				page.getPage(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isHasNext()
		);
	}
}
