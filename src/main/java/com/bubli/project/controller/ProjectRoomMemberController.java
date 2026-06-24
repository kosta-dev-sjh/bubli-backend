package com.bubli.project.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.response.PageResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.project.dto.CreateInvitationRequest;
import com.bubli.project.dto.InvitationResponse;
import com.bubli.project.dto.InvitationResult;
import com.bubli.project.dto.ProjectRoomMemberResponse;
import com.bubli.project.dto.ProjectRoomMemberResult;
import com.bubli.project.dto.UpdateRoomMemberRoleRequest;
import com.bubli.project.service.ProjectRoomMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProjectRoomMemberController {

	private final ProjectRoomMemberService projectRoomMemberService;

	@GetMapping("/api/project-rooms/{roomId}/members")
	public ApiResponse<PageResponse<ProjectRoomMemberResponse>> getMembers(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(mapMemberPage(projectRoomMemberService.getMembers(authUser.userId(), roomId, pageable)));
	}

	@PostMapping("/api/project-rooms/{roomId}/invitations")
	public ApiResponse<InvitationResponse> createInvitation(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@Valid @RequestBody CreateInvitationRequest request
	) {
		return ApiResponse.success(InvitationResponse.from(
				projectRoomMemberService.createInvitation(authUser.userId(), roomId, request.toCommand())
		));
	}

	@GetMapping("/api/project-rooms/{roomId}/invitations")
	public ApiResponse<PageResponse<InvitationResponse>> getInvitations(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(mapInvitationPage(projectRoomMemberService.getInvitations(authUser.userId(), roomId, pageable)));
	}

	@PatchMapping("/api/invitations/{invitationId}/accept")
	public ApiResponse<InvitationResponse> acceptInvitation(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID invitationId
	) {
		return ApiResponse.success(InvitationResponse.from(
				projectRoomMemberService.acceptInvitation(authUser.userId(), invitationId)
		));
	}

	@PatchMapping("/api/invitations/{invitationId}/cancel")
	public ApiResponse<InvitationResponse> cancelInvitation(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID invitationId
	) {
		return ApiResponse.success(InvitationResponse.from(
				projectRoomMemberService.cancelInvitation(authUser.userId(), invitationId)
		));
	}

	@PatchMapping("/api/project-rooms/{roomId}/members/{userId}")
	public ApiResponse<ProjectRoomMemberResponse> updateMemberRole(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@PathVariable UUID userId,
			@Valid @RequestBody UpdateRoomMemberRoleRequest request
	) {
		return ApiResponse.success(ProjectRoomMemberResponse.from(
				projectRoomMemberService.updateMemberRole(authUser.userId(), roomId, userId, request.role())
		));
	}

	@DeleteMapping("/api/project-rooms/{roomId}/members/{userId}")
	public ApiResponse<Void> removeMember(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@PathVariable UUID userId
	) {
		projectRoomMemberService.removeMember(authUser.userId(), roomId, userId);
		return ApiResponse.success(null);
	}

	private PageResponse<ProjectRoomMemberResponse> mapMemberPage(PageResponse<ProjectRoomMemberResult> page) {
		return new PageResponse<>(
				page.getItems().stream()
						.map(ProjectRoomMemberResponse::from)
						.toList(),
				page.getPage(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isHasNext()
		);
	}

	private PageResponse<InvitationResponse> mapInvitationPage(PageResponse<InvitationResult> page) {
		return new PageResponse<>(
				page.getItems().stream()
						.map(InvitationResponse::from)
						.toList(),
				page.getPage(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isHasNext()
		);
	}
}
