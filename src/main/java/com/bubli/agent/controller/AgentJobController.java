package com.bubli.agent.controller;

import com.bubli.agent.dto.AgentJobResponse;
import com.bubli.agent.service.AgentJobService;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AgentJobController {

	private final AgentJobService agentJobService;

	@GetMapping("/api/agent-jobs/{jobId}")
	public ApiResponse<AgentJobResponse> getAgentJob(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID jobId
	) {
		return ApiResponse.success(AgentJobResponse.from(
				agentJobService.getRequestedJob(authUser.userId(), jobId)
		));
	}
}
