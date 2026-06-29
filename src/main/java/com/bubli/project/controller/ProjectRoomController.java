package com.bubli.project.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.response.PageResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.project.dto.CreateProjectRoomRequest;
import com.bubli.project.dto.ProjectRoomEventResponse;
import com.bubli.project.dto.ProjectRoomResponse;
import com.bubli.project.dto.ProjectRoomResult;
import com.bubli.project.dto.UpdateProjectRoomPaymentRequest;
import com.bubli.project.dto.UpdateProjectRoomRequest;
import com.bubli.project.service.ProjectRoomEventService;
import com.bubli.project.service.ProjectRoomService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProjectRoomController {

	private final ProjectRoomService projectRoomService;
	private final ProjectRoomEventService projectRoomEventService;

	@GetMapping("/api/project-rooms")
	public ApiResponse<PageResponse<ProjectRoomResponse>> getProjectRooms(
			@CurrentUser AuthUser authUser,
			@PageableDefault(size = 20) Pageable pageable
	) {
		PageResponse<ProjectRoomResponse> response = mapPage(projectRoomService.getProjectRooms(authUser.userId(), pageable));
		return ApiResponse.success(response);
	}

	@GetMapping("/api/me/project-rooms")
	public ApiResponse<PageResponse<ProjectRoomResponse>> getMyProjectRooms(
			@CurrentUser AuthUser authUser,
			@PageableDefault(size = 20) Pageable pageable
	) {
		PageResponse<ProjectRoomResponse> response = mapPage(projectRoomService.getProjectRooms(authUser.userId(), pageable));
		return ApiResponse.success(response);
	}

	@PostMapping("/api/project-rooms")
	public ApiResponse<ProjectRoomResponse> createProjectRoom(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody CreateProjectRoomRequest request
	) {
		return ApiResponse.success(ProjectRoomResponse.from(projectRoomService.create(authUser.userId(), request.toCommand())));
	}

	@GetMapping("/api/project-rooms/{roomId}")
	public ApiResponse<ProjectRoomResponse> getProjectRoom(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId
	) {
		return ApiResponse.success(ProjectRoomResponse.from(projectRoomService.getProjectRoom(authUser.userId(), roomId)));
	}

	@PatchMapping("/api/project-rooms/{roomId}")
	public ApiResponse<ProjectRoomResponse> updateProjectRoom(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@Valid @RequestBody UpdateProjectRoomRequest request
	) {
		return ApiResponse.success(ProjectRoomResponse.from(
				projectRoomService.updateProjectRoom(authUser.userId(), roomId, request.toCommand())
		));
	}

	@PatchMapping("/api/project-rooms/{roomId}/payment")
	public ApiResponse<ProjectRoomResponse> updateProjectRoomPayment(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@Valid @RequestBody UpdateProjectRoomPaymentRequest request
	) {
		return ApiResponse.success(ProjectRoomResponse.from(
				projectRoomService.updateProjectRoomPayment(authUser.userId(), roomId, request.toCommand())
		));
	}

	@DeleteMapping("/api/project-rooms/{roomId}")
	public ApiResponse<ProjectRoomResponse> closeProjectRoom(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId
	) {
		return ApiResponse.success(ProjectRoomResponse.from(projectRoomService.closeProjectRoom(authUser.userId(), roomId)));
	}

	@GetMapping("/api/project-rooms/{roomId}/events")
	public ApiResponse<PageResponse<ProjectRoomEventResponse>> getProjectRoomEvents(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@RequestParam(required = false) Long afterSequence,
			@RequestParam(required = false) Integer limit
	) {
		return ApiResponse.success(projectRoomEventService.getEvents(
				authUser.userId(),
				roomId,
				afterSequence,
				limit
		));
	}


	private PageResponse<ProjectRoomResponse> mapPage(PageResponse<ProjectRoomResult> page) {
		return new PageResponse<>(
				page.getItems().stream()
						.map(ProjectRoomResponse::from)
						.toList(),
				page.getPage(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isHasNext()
		);
	}
}
