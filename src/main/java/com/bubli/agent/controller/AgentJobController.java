package com.bubli.agent.controller;

import com.bubli.agent.dto.AgentJobEventResponse;
import com.bubli.agent.dto.AgentJobEventResult;
import com.bubli.agent.dto.AgentJobResponse;
import com.bubli.agent.service.AgentJobService;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.response.PageResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

	@GetMapping("/api/agent-jobs/{jobId}/events")
	public ApiResponse<PageResponse<AgentJobEventResponse>> getAgentJobEvents(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID jobId,
			@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(mapEventPage(
				agentJobService.getRequestedJobEvents(authUser.userId(), jobId, pageable)
		));
	}

	private PageResponse<AgentJobEventResponse> mapEventPage(PageResponse<AgentJobEventResult> page) {
		return new PageResponse<>(
				page.getItems().stream()
						.map(AgentJobEventResponse::from)
						.toList(),
				page.getPage(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isHasNext()
		);
	}
}
