package com.bubli.user.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.user.dto.FriendRequestResponse;
import com.bubli.user.dto.SendFriendRequestRequest;
import com.bubli.user.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FriendRequestController {

	private final FriendService friendService;

	@GetMapping("/api/friend-requests")
	public ApiResponse<List<FriendRequestResponse>> getFriendRequests(@CurrentUser AuthUser authUser) {
		return ApiResponse.success(friendService.getFriendRequests(authUser.userId()));
	}

	@PostMapping("/api/friend-requests")
	public ApiResponse<FriendRequestResponse> sendFriendRequest(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody SendFriendRequestRequest request
	) {
		return ApiResponse.success(friendService.sendFriendRequest(authUser.userId(), request.bubliId()));
	}

	@PatchMapping("/api/friend-requests/{id}/accept")
	public ApiResponse<FriendRequestResponse> acceptFriendRequest(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID id
	) {
		return ApiResponse.success(friendService.acceptFriendRequest(authUser.userId(), id));
	}

	@PatchMapping("/api/friend-requests/{id}/reject")
	public ApiResponse<FriendRequestResponse> rejectFriendRequest(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID id
	) {
		return ApiResponse.success(friendService.rejectFriendRequest(authUser.userId(), id));
	}
}
