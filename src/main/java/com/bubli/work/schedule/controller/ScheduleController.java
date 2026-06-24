package com.bubli.work.schedule.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.response.PageResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.work.schedule.dto.CreateScheduleRequest;
import com.bubli.work.schedule.dto.ScheduleResponse;
import com.bubli.work.schedule.dto.UpdateScheduleRequest;
import com.bubli.work.schedule.entity.Schedule;
import com.bubli.work.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ScheduleController {

	private final ScheduleService scheduleService;

	@GetMapping("/api/schedules")
	public ApiResponse<PageResponse<ScheduleResponse>> getSchedules(
			@CurrentUser AuthUser authUser,
			@RequestParam(required = false) UUID roomId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			@PageableDefault(size = 50) Pageable pageable
	) {
		PageResponse<Schedule> page = scheduleService.getSchedules(authUser.userId(), roomId, from, to, pageable);
		return ApiResponse.success(mapPage(page));
	}

	@PostMapping("/api/schedules")
	public ApiResponse<ScheduleResponse> createSchedule(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody CreateScheduleRequest request
	) {
		return ApiResponse.success(ScheduleResponse.from(scheduleService.create(authUser.userId(), request.toCommand())));
	}

	@PatchMapping("/api/schedules/{scheduleId}")
	public ApiResponse<ScheduleResponse> updateSchedule(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID scheduleId,
			@Valid @RequestBody UpdateScheduleRequest request
	) {
		return ApiResponse.success(ScheduleResponse.from(
				scheduleService.update(authUser.userId(), scheduleId, request.toCommand())
		));
	}

	@DeleteMapping("/api/schedules/{scheduleId}")
	public ApiResponse<Void> deleteSchedule(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID scheduleId
	) {
		scheduleService.delete(authUser.userId(), scheduleId);
		return ApiResponse.success(null);
	}

	private PageResponse<ScheduleResponse> mapPage(PageResponse<Schedule> page) {
		return new PageResponse<>(
				page.getItems().stream()
						.map(ScheduleResponse::from)
						.toList(),
				page.getPage(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isHasNext()
		);
	}
}
