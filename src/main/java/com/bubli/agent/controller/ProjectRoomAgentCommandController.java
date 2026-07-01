package com.bubli.agent.controller;

import com.bubli.agent.dto.ProjectRoomAgentCommandRequest;
import com.bubli.agent.dto.ProjectRoomAgentCommandResponse;
import com.bubli.agent.service.ProjectRoomAgentCommandService;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProjectRoomAgentCommandController {

	private final ProjectRoomAgentCommandService projectRoomAgentCommandService;

	@PostMapping("/api/project-rooms/{roomId}/agent/commands")
	public ApiResponse<ProjectRoomAgentCommandResponse> execute(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@Valid @RequestBody ProjectRoomAgentCommandRequest request
	) {
		return ApiResponse.success(projectRoomAgentCommandService.execute(
				authUser.userId(),
				roomId,
				request.message(),
				request.mode(),
				request.resourceIds()
		));
	}
}
