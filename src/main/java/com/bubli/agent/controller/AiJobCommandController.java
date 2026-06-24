package com.bubli.agent.controller;

import com.bubli.agent.dto.AgentJobResponse;
import com.bubli.agent.dto.AnalyzeResourceRequest;
import com.bubli.agent.dto.CreateRoomAgentJobRequest;
import com.bubli.agent.service.AiJobCommandService;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AiJobCommandController {

	private final AiJobCommandService aiJobCommandService;

	@PostMapping("/api/ai/analyze-resource")
	public ApiResponse<AgentJobResponse> analyzeResource(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody AnalyzeResourceRequest request
	) {
		return ApiResponse.success(AgentJobResponse.from(
				aiJobCommandService.createAnalyzeResourceJob(authUser.userId(), request.resourceId())
		));
	}

	@PostMapping("/api/ai/generate-requirements")
	public ApiResponse<AgentJobResponse> generateRequirements(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody CreateRoomAgentJobRequest request
	) {
		return ApiResponse.success(AgentJobResponse.from(
				aiJobCommandService.createGenerateRequirementsJob(authUser.userId(), request.roomId())
		));
	}

	@PostMapping("/api/ai/generate-tasks")
	public ApiResponse<AgentJobResponse> generateTasks(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody CreateRoomAgentJobRequest request
	) {
		return ApiResponse.success(AgentJobResponse.from(
				aiJobCommandService.createGenerateTasksJob(authUser.userId(), request.roomId())
		));
	}

	@PostMapping("/api/ai/generate-wbs")
	public ApiResponse<AgentJobResponse> generateWbs(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody CreateRoomAgentJobRequest request
	) {
		return ApiResponse.success(AgentJobResponse.from(
				aiJobCommandService.createGenerateWbsJob(authUser.userId(), request.roomId())
		));
	}

	@PostMapping("/api/ai/generate-questions")
	public ApiResponse<AgentJobResponse> generateQuestions(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody CreateRoomAgentJobRequest request
	) {
		return ApiResponse.success(AgentJobResponse.from(
				aiJobCommandService.createGenerateQuestionsJob(authUser.userId(), request.roomId())
		));
	}
}
