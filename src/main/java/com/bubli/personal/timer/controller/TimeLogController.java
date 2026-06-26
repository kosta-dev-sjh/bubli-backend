package com.bubli.personal.timer.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.personal.timer.dto.StartTimeLogCommand;
import com.bubli.personal.timer.dto.StartTimeLogRequest;
import com.bubli.personal.timer.dto.TimeLogResponse;
import com.bubli.personal.timer.service.TimeLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TimeLogController {

	private final TimeLogService timeLogService;

	@PostMapping("/api/time-logs/start")
	public ApiResponse<TimeLogResponse> start(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody StartTimeLogRequest request
	) {
		return ApiResponse.success(TimeLogResponse.from(
				timeLogService.start(StartTimeLogCommand.of(authUser.userId(), request))
		));
	}

	@PatchMapping("/api/time-logs/{timeLogId}/pause")
	public ApiResponse<TimeLogResponse> pause(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID timeLogId
	) {
		return ApiResponse.success(TimeLogResponse.from(timeLogService.pause(authUser.userId(), timeLogId)));
	}

	@PatchMapping("/api/time-logs/{timeLogId}/resume")
	public ApiResponse<TimeLogResponse> resume(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID timeLogId
	) {
		return ApiResponse.success(TimeLogResponse.from(timeLogService.resume(authUser.userId(), timeLogId)));
	}

	@PatchMapping("/api/time-logs/{timeLogId}/stop")
	public ApiResponse<TimeLogResponse> stop(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID timeLogId
	) {
		return ApiResponse.success(TimeLogResponse.from(timeLogService.stop(authUser.userId(), timeLogId)));
	}

	@PatchMapping("/api/time-logs/{timeLogId}/heartbeat")
	public ApiResponse<TimeLogResponse> heartbeat(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID timeLogId
	) {
		return ApiResponse.success(TimeLogResponse.from(timeLogService.heartbeat(authUser.userId(), timeLogId)));
	}
}
