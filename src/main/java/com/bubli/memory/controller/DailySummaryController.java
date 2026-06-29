package com.bubli.memory.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.response.PageResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.memory.dto.DailySummaryPatchRequest;
import com.bubli.memory.dto.DailySummaryResponse;
import com.bubli.memory.service.DailySummaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DailySummaryController {

    private final DailySummaryService dailySummaryService;

    @GetMapping("/api/daily-summaries")
    public ApiResponse<PageResponse<DailySummaryResponse>> getDailySummaries(
            @CurrentUser AuthUser authUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(dailySummaryService.getDailySummaries(authUser.userId(), pageable));
    }

    @PatchMapping("/api/daily-summaries/{summaryId}")
    public ApiResponse<DailySummaryResponse> patchDailySummary(
            @CurrentUser AuthUser authUser,
            @PathVariable UUID summaryId,
            @Valid @RequestBody DailySummaryPatchRequest request
    ) {
        return ApiResponse.success(dailySummaryService.patch(authUser.userId(), summaryId, request.toCommand()));
    }
}
