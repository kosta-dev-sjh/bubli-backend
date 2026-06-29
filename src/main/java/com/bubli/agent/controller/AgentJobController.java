package com.bubli.agent.controller;

import com.bubli.agent.dto.AgentJobResponse;
import com.bubli.agent.dto.SearchResourceRequest;
import com.bubli.agent.dto.SearchResourceResponse;
import com.bubli.agent.service.AgentJobQueryService;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.resource.service.ResourceSemanticSearchPublicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AgentJobController {

    private final AgentJobQueryService agentJobQueryService;
    private final ResourceSemanticSearchPublicService resourceSemanticSearchService;

    @GetMapping("/api/agent-jobs/{jobId}")
    public ResponseEntity<ApiResponse<AgentJobResponse>> getJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.success(agentJobQueryService.getJob(jobId)));
    }

    @PostMapping("/api/ai/search-resource")
    public ResponseEntity<ApiResponse<SearchResourceResponse>> searchResource(
            @Valid @RequestBody SearchResourceRequest request,
            @CurrentUser AuthUser currentUser
    ) {
        SearchResourceResponse response = SearchResourceResponse.of(
                resourceSemanticSearchService.search(
                        currentUser.userId(),
                        request.scope(),
                        request.roomId(),
                        request.query(),
                        request.topK()
                )
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
