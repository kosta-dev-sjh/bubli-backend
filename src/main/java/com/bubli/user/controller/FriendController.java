package com.bubli.user.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.user.dto.FriendResponse;
import com.bubli.user.dto.UserSearchResponse;
import com.bubli.user.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FriendController {

	private final FriendService friendService;

	@GetMapping("/api/friends")
	public ApiResponse<List<FriendResponse>> getFriends(@CurrentUser AuthUser authUser) {
		return ApiResponse.success(friendService.getFriends(authUser.userId()));
	}

	@GetMapping("/api/friends/search")
	public ApiResponse<UserSearchResponse> searchFriends(
			@CurrentUser AuthUser authUser,
			@RequestParam String bubliId
	) {
		return ApiResponse.success(friendService.searchByBubliId(authUser.userId(), bubliId));
	}

	@DeleteMapping("/api/friends/{friendUserId}")
	public ApiResponse<Void> deleteFriend(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID friendUserId
	) {
		friendService.deleteFriend(authUser.userId(), friendUserId);
		return ApiResponse.success(null);
	}
}
