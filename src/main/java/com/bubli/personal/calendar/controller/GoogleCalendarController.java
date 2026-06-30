package com.bubli.personal.calendar.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.response.PageResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.personal.calendar.dto.CalendarEventRequest;
import com.bubli.personal.calendar.dto.CalendarEventResponse;
import com.bubli.personal.calendar.dto.CalendarEventUpdateRequest;
import com.bubli.personal.calendar.dto.GoogleCalendarCallbackRequest;
import com.bubli.personal.calendar.dto.GoogleCalendarConnectResponse;
import com.bubli.personal.calendar.dto.GoogleCalendarConnectionResponse;
import com.bubli.personal.calendar.service.GoogleCalendarConnectionService;
import com.bubli.personal.calendar.service.GoogleCalendarEventService;
import com.bubli.work.schedule.dto.ScheduleResponse;
import com.bubli.work.schedule.dto.ScheduleResult;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class GoogleCalendarController {

	private final GoogleCalendarConnectionService connectionService;
	private final GoogleCalendarEventService eventService;

	@GetMapping("/api/calendar/google/connect")
	public ApiResponse<GoogleCalendarConnectResponse> connectUrl(
			@CurrentUser AuthUser authUser,
			@RequestParam(required = false) String redirectUri
	) {
		return ApiResponse.success(connectionService.createConnectUrl(authUser.userId(), redirectUri));
	}

	@PostMapping("/api/calendar/google/callback")
	public ApiResponse<GoogleCalendarConnectionResponse> callback(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody GoogleCalendarCallbackRequest request
	) {
		return ApiResponse.success(connectionService.connect(authUser.userId(), request.code(), request.redirectUri()));
	}

	@GetMapping("/api/calendar/google/connection")
	public ApiResponse<GoogleCalendarConnectionResponse> getConnection(@CurrentUser AuthUser authUser) {
		return ApiResponse.success(connectionService.getConnection(authUser.userId()).orElse(null));
	}

	@DeleteMapping("/api/calendar/google/connection")
	public ApiResponse<Void> disconnect(@CurrentUser AuthUser authUser) {
		connectionService.disconnect(authUser.userId());
		return ApiResponse.success(null);
	}

	@GetMapping("/api/calendar/events")
	public ApiResponse<PageResponse<ScheduleResponse>> getEvents(
			@CurrentUser AuthUser authUser,
			@RequestParam(required = false) UUID roomId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			@PageableDefault(size = 50) Pageable pageable
	) {
		return ApiResponse.success(mapPage(eventService.getEvents(authUser.userId(), roomId, from, to, pageable)));
	}

	@PostMapping("/api/calendar/events")
	public ApiResponse<CalendarEventResponse> createEvent(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody CalendarEventRequest request
	) {
		ScheduleResult schedule = eventService.createEvent(authUser.userId(), request.toCommand());
		return ApiResponse.success(CalendarEventResponse.from(schedule, eventService.hasActiveConnection(authUser.userId())));
	}

	@PatchMapping("/api/calendar/events/{scheduleId}")
	public ApiResponse<CalendarEventResponse> updateEvent(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID scheduleId,
			@Valid @RequestBody CalendarEventUpdateRequest request
	) {
		ScheduleResult schedule = eventService.updateEvent(authUser.userId(), scheduleId, request.toCommand());
		return ApiResponse.success(CalendarEventResponse.from(schedule, eventService.hasActiveConnection(authUser.userId())));
	}

	@DeleteMapping("/api/calendar/events/{scheduleId}")
	public ApiResponse<Void> deleteEvent(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID scheduleId
	) {
		eventService.deleteEvent(authUser.userId(), scheduleId);
		return ApiResponse.success(null);
	}

	@PostMapping("/api/calendar/sync")
	public ApiResponse<List<ScheduleResponse>> syncEvents(
			@CurrentUser AuthUser authUser,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
	) {
		return ApiResponse.success(eventService.syncEvents(authUser.userId(), from, to).stream()
				.map(ScheduleResponse::from)
				.toList());
	}

	@PostMapping("/api/calendar/push-unsynced")
	public ApiResponse<List<ScheduleResponse>> pushUnsyncedEvents(
			@CurrentUser AuthUser authUser,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
	) {
		return ApiResponse.success(eventService.pushUnsyncedEvents(authUser.userId(), from, to).stream()
				.map(ScheduleResponse::from)
				.toList());
	}

	private PageResponse<ScheduleResponse> mapPage(PageResponse<ScheduleResult> page) {
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
