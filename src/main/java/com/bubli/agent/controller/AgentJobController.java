package com.bubli.agent.controller;

import com.bubli.agent.dto.AgentJobResponse;
import com.bubli.agent.service.AgentJobQueryService;
import com.bubli.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class
AgentJobController {

    private final AgentJobQueryService agentJobQueryService;

    @GetMapping("/api/agent-jobs/{jobId}")
    public ResponseEntity<ApiResponse<AgentJobResponse>> getJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.success(agentJobQueryService.getJob(jobId)));
    }
}
