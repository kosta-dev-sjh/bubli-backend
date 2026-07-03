package com.bubli.localsync.controller;

import com.bubli.agent.dto.AgentJobResponse;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.localsync.dto.LocalFileAnalysisRequest;
import com.bubli.localsync.service.LocalFileAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LocalFileAnalysisController {

    private final LocalFileAnalysisService localFileAnalysisService;

    @PostMapping("/api/local-file-analyses")
    public ApiResponse<AgentJobResponse> analyzeLocalFile(
            @CurrentUser AuthUser authUser,
            @Valid @RequestBody LocalFileAnalysisRequest request
    ) {
        return ApiResponse.success(AgentJobResponse.from(
                localFileAnalysisService.requestAnalysis(authUser.userId(), request.toCommand())
        ));
    }
}
