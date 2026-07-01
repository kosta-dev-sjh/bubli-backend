package com.bubli.activity.controller;

import com.bubli.activity.dto.ActivityLogResponse;
import com.bubli.activity.dto.RecordCurrentAppActivityRequest;
import com.bubli.activity.service.ActivityService;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ActivityController {

	private final ActivityService activityService;

	@PostMapping("/api/activity/current-app")
	public ApiResponse<ActivityLogResponse> recordCurrentApp(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody RecordCurrentAppActivityRequest request
	) {
		return ApiResponse.success(ActivityLogResponse.from(
				activityService.recordCurrentApp(authUser.userId(), request.toCommand())
		));
	}

	@GetMapping("/api/activity/today")
	public ApiResponse<List<ActivityLogResponse>> getTodayActivities(@CurrentUser AuthUser authUser) {
		return ApiResponse.success(activityService.getTodayActivities(authUser.userId()).stream()
				.map(ActivityLogResponse::from)
				.toList());
	}

	@DeleteMapping("/api/activity/{activityId}")
	public ApiResponse<Void> deleteActivity(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID activityId
	) {
		activityService.deleteActivity(authUser.userId(), activityId);
		return ApiResponse.success(null);
	}
}
