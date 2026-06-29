package com.bubli.agent.controller;

import com.bubli.agent.dto.AgentJobResponse;
import com.bubli.agent.dto.AgentJobEventResponse;
import com.bubli.agent.dto.AgentJobEventResult;
import com.bubli.agent.dto.SearchResourceRequest;
import com.bubli.agent.dto.SearchResourceResponse;
import com.bubli.agent.service.AgentJobQueryService;
import com.bubli.agent.service.AgentJobService;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.response.PageResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.resource.service.ResourceSemanticSearchPublicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    private final AgentJobService agentJobService;
    private final ResourceSemanticSearchPublicService resourceSemanticSearchService;

    @GetMapping("/api/agent-jobs/{jobId}")
    public ResponseEntity<ApiResponse<AgentJobResponse>> getJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.success(agentJobQueryService.getJob(jobId)));
    }

    @GetMapping("/api/agent-jobs/{jobId}/events")
    public ResponseEntity<ApiResponse<PageResponse<AgentJobEventResponse>>> getJobEvents(
            @PathVariable UUID jobId,
            @CurrentUser AuthUser currentUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<AgentJobEventResult> events = agentJobService.getAccessibleJobEvents(
                currentUser.userId(),
                jobId,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.success(mapEventPage(events)));
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
