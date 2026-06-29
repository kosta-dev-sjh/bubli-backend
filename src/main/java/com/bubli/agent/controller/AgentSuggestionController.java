package com.bubli.agent.controller;

import com.bubli.agent.dto.AgentSuggestionResponse;
import com.bubli.agent.dto.AgentSuggestionUpdateRequest;
import com.bubli.agent.service.AgentSuggestionCommandService;
import com.bubli.agent.service.AgentSuggestionQueryService;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AgentSuggestionController {

    private final AgentSuggestionQueryService agentSuggestionQueryService;
    private final AgentSuggestionCommandService agentSuggestionCommandService;

    @GetMapping("/api/agent/suggestions")
    public ResponseEntity<ApiResponse<List<AgentSuggestionResponse>>> findMine(
            @RequestParam(required = false) AgentSuggestionStatus status,
            @RequestParam(required = false) AgentSuggestionType suggestionType,
            @CurrentUser AuthUser currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(agentSuggestionQueryService.findMine(
                currentUser.userId(),
                status,
                suggestionType
        )));
    }

    @GetMapping("/api/project-rooms/{roomId}/agent/suggestions")
    public ResponseEntity<ApiResponse<List<AgentSuggestionResponse>>> findRoomSuggestions(
            @PathVariable UUID roomId,
            @RequestParam(required = false) AgentSuggestionStatus status,
            @RequestParam(required = false) AgentSuggestionType suggestionType
    ) {
        return ResponseEntity.ok(ApiResponse.success(agentSuggestionQueryService.findRoomSuggestions(
                roomId,
                status,
                suggestionType
        )));
    }

    @PatchMapping("/api/agent/suggestions/{suggestionId}")
    public ResponseEntity<ApiResponse<AgentSuggestionResponse>> review(
            @PathVariable UUID suggestionId,
            @Valid @RequestBody AgentSuggestionUpdateRequest request,
            @CurrentUser AuthUser currentUser
    ) {
        AgentSuggestionResponse response = agentSuggestionCommandService.review(
                suggestionId,
                currentUser.userId(),
                request.action(),
                request.payloadJson()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
