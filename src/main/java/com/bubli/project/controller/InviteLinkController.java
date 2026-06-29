package com.bubli.project.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.project.dto.CreateInviteLinkRequest;
import com.bubli.project.dto.InviteLinkResponse;
import com.bubli.project.service.InviteLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class InviteLinkController {

	private final InviteLinkService inviteLinkService;

	@PostMapping("/api/project-rooms/{roomId}/invite-links")
	public ApiResponse<InviteLinkResponse> createInviteLink(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@RequestBody(required = false) CreateInviteLinkRequest request
	) {
		int hours = request != null ? request.expiresInHours() : 72;
		return ApiResponse.success(inviteLinkService.createInviteLink(authUser.userId(), roomId, hours));
	}

	@GetMapping("/api/invite-links/{token}")
	public ApiResponse<InviteLinkResponse> getInviteLink(
			@PathVariable String token
	) {
		return ApiResponse.success(inviteLinkService.getInviteLink(token));
	}

	@PatchMapping("/api/invite-links/{token}/accept")
	public ApiResponse<InviteLinkResponse> acceptInviteLink(
			@CurrentUser AuthUser authUser,
			@PathVariable String token
	) {
		return ApiResponse.success(inviteLinkService.acceptInviteLink(authUser.userId(), token));
	}
}
