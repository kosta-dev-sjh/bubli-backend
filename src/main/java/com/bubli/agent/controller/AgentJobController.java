package com.bubli.agent.controller;

import com.bubli.agent.dto.AgentJobResponse;
import com.bubli.agent.dto.AnalyzeResourceRequest;
import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.service.AgentJobCommandService;
import com.bubli.agent.service.AgentJobQueryService;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AgentJobController {

    private final AgentJobCommandService agentJobCommandService;
    private final AgentJobQueryService agentJobQueryService;

    @PostMapping("/api/ai/analyze-resource")
    public ResponseEntity<ApiResponse<AgentJobResponse>> analyzeResource(
            @Valid @RequestBody AnalyzeResourceRequest request,
            @CurrentUser AuthUser currentUser
    ) {
        AgentJob job = agentJobCommandService.createAnalyzeResourceJob(
                currentUser.userId(),
                request.resourceId()
        );
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(AgentJobResponse.of(job, List.of(), null, null)));
    }

    @GetMapping("/api/agent-jobs/{jobId}")
    public ResponseEntity<ApiResponse<AgentJobResponse>> getJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.success(agentJobQueryService.getJob(jobId)));
    }
}
