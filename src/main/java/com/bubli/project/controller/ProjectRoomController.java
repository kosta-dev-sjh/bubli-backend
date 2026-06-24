package com.bubli.project.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.response.PageResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.project.dto.CreateProjectRoomRequest;
import com.bubli.project.dto.ProjectRoomResponse;
import com.bubli.project.dto.ProjectRoomResult;
import com.bubli.project.service.ProjectRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProjectRoomController {

	private final ProjectRoomService projectRoomService;

	@GetMapping("/api/project-rooms")
	public ApiResponse<PageResponse<ProjectRoomResponse>> getProjectRooms(
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
