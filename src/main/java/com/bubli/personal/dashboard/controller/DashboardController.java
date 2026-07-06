package com.bubli.personal.dashboard.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.personal.dashboard.dto.DashboardActivityHeatmapResponse;
import com.bubli.personal.dashboard.dto.DashboardWorkResponse;
import com.bubli.personal.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DashboardController {

	private final DashboardService dashboardService;

	@GetMapping("/api/dashboard/work")
	public ApiResponse<DashboardWorkResponse> getWorkDashboard(@CurrentUser AuthUser authUser) {
		return ApiResponse.success(dashboardService.getWorkDashboard(authUser.userId()));
	}

	@GetMapping("/api/dashboard/activity-heatmap")
	public ApiResponse<List<DashboardActivityHeatmapResponse>> getActivityHeatmap(
			@CurrentUser AuthUser authUser,
			@RequestParam(defaultValue = "365") int days) {
		return ApiResponse.success(dashboardService.getActivityHeatmap(authUser.userId(), days));
	}
}
