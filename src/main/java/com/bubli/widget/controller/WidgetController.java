package com.bubli.widget.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.widget.dto.BubbleSettingUpdate;
import com.bubli.widget.dto.SaveUsageSummaryRequest;
import com.bubli.widget.dto.UpdateWidgetContextRequest;
import com.bubli.widget.dto.UpdateWidgetItemStateRequest;
import com.bubli.widget.dto.UpdateWidgetSettingsRequest;
import com.bubli.widget.dto.WidgetContextResponse;
import com.bubli.widget.dto.WidgetDailySummaryResponse;
import com.bubli.widget.dto.WidgetSettingsResponse;
import com.bubli.widget.dto.WidgetSummaryResponse;
import com.bubli.widget.dto.WidgetTodaySummaryResponse;
import com.bubli.widget.service.WidgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/widget")
@RequiredArgsConstructor
public class WidgetController {

    private final WidgetService widgetService;

    @GetMapping("/summary")
    public ApiResponse<WidgetSummaryResponse> getSummary(@CurrentUser AuthUser authUser) {
        return ApiResponse.success(widgetService.getSummary(authUser.userId()));
    }

    @GetMapping("/settings")
    public ApiResponse<WidgetSettingsResponse> getSettings(@CurrentUser AuthUser authUser) {
        return ApiResponse.success(widgetService.getSettings(authUser.userId()));
    }

    @PatchMapping("/settings")
    public ApiResponse<WidgetSettingsResponse> updateSettings(
            @CurrentUser AuthUser authUser,
            @RequestBody @Valid UpdateWidgetSettingsRequest request
    ) {
        List<BubbleSettingUpdate> updates = request.bubbles().stream()
                .map(r -> new BubbleSettingUpdate(r.bubbleType(), r.enabled(), r.x(), r.y(),
                        r.width(), r.height(), r.minimized(), r.opacity(), r.ghostMode(), r.alertEnabled()))
                .toList();
        return ApiResponse.success(widgetService.updateSettings(authUser.userId(), updates));
    }

    @GetMapping("/context")
    public ApiResponse<WidgetContextResponse> getContext(@CurrentUser AuthUser authUser) {
        return ApiResponse.success(widgetService.getContext(authUser.userId()));
    }

    @PatchMapping("/context")
    public ApiResponse<WidgetContextResponse> updateContext(
            @CurrentUser AuthUser authUser,
            @RequestBody UpdateWidgetContextRequest request
    ) {
        return ApiResponse.success(widgetService.updateContext(authUser.userId(), request.selectedRoomId()));
    }

    @PatchMapping("/items/{id}/state")
    public ApiResponse<Void> updateItemState(
            @CurrentUser AuthUser authUser,
            @PathVariable UUID id,
            @RequestBody @Valid UpdateWidgetItemStateRequest request
    ) {
        widgetService.updateItemState(authUser.userId(), id, request.state());
        return ApiResponse.success(null);
    }

    @PostMapping("/usage-summaries")
    public ApiResponse<WidgetDailySummaryResponse> saveUsageSummary(
            @CurrentUser AuthUser authUser,
            @RequestBody @Valid SaveUsageSummaryRequest request
    ) {
        return ApiResponse.success(widgetService.saveUsageSummary(
                authUser.userId(), request.deviceId(), request.rollupKey(),
                request.summaryDate(), request.bubbleSettingId(),
                request.openCount(), request.interactionCount(), request.visibleSeconds(), request.syncedAt()));
    }

    @GetMapping("/usage-summaries/today")
    public ApiResponse<WidgetTodaySummaryResponse> getTodaySummary(@CurrentUser AuthUser authUser) {
        return ApiResponse.success(widgetService.getTodaySummary(authUser.userId()));
    }
}
