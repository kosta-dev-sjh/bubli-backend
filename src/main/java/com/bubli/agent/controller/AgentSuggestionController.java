package com.bubli.agent.controller;

import com.bubli.agent.dto.AgentSuggestionResponse;
import com.bubli.agent.dto.AgentSuggestionResult;
import com.bubli.agent.dto.UpdateAgentSuggestionRequest;
import com.bubli.agent.service.AgentSuggestionService;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.response.PageResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AgentSuggestionController {

	private final AgentSuggestionService agentSuggestionService;

	@GetMapping("/api/agent/suggestions")
	public ApiResponse<PageResponse<AgentSuggestionResponse>> getPersonalSuggestions(
			@CurrentUser AuthUser authUser,
			@RequestParam(defaultValue = "DRAFT") AgentSuggestionStatus status,
			@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(mapPage(
				agentSuggestionService.getPersonalSuggestions(authUser.userId(), status, pageable)
		));
	}

	@GetMapping("/api/project-rooms/{roomId}/agent/suggestions")
	public ApiResponse<PageResponse<AgentSuggestionResponse>> getRoomSuggestions(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@RequestParam(defaultValue = "DRAFT") AgentSuggestionStatus status,
			@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(mapPage(
				agentSuggestionService.getRoomSuggestions(authUser.userId(), roomId, status, pageable)
		));
	}

	@PatchMapping("/api/agent/suggestions/{suggestionId}")
	public ApiResponse<AgentSuggestionResponse> updateSuggestion(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID suggestionId,
			@Valid @RequestBody UpdateAgentSuggestionRequest request
	) {
		return ApiResponse.success(AgentSuggestionResponse.from(
				agentSuggestionService.updateSuggestion(authUser.userId(), suggestionId, request.toCommand())
		));
	}

	private PageResponse<AgentSuggestionResponse> mapPage(PageResponse<AgentSuggestionResult> page) {
		return new PageResponse<>(
				page.getItems().stream()
						.map(AgentSuggestionResponse::from)
						.toList(),
				page.getPage(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isHasNext()
		);
	}
}
