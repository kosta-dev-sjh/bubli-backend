package com.bubli.agent.controller;

import com.bubli.agent.dto.ProjectRoomRagEvaluationRequest;
import com.bubli.agent.dto.ProjectRoomRagEvaluationResponse;
import com.bubli.agent.service.ProjectRoomGroundingService;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.project.service.ProjectMembershipPublicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProjectRoomRagEvaluationController {

	private final ProjectMembershipPublicService projectMembershipPublicService;
	private final ProjectRoomGroundingService groundingService;

	@PostMapping("/api/ai/evaluate-project-room-rag")
	public ApiResponse<ProjectRoomRagEvaluationResponse> evaluate(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody ProjectRoomRagEvaluationRequest request
	) {
		projectMembershipPublicService.assertActiveMember(authUser.userId(), request.roomId());
		return ApiResponse.success(ProjectRoomRagEvaluationResponse.from(groundingService.retrieve(
				authUser.userId(),
				request.roomId(),
				request.message(),
				request.locale(),
				request.mode()
		)));
	}
}
