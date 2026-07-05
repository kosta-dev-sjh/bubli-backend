package com.bubli.personal.calendar.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.personal.calendar.dto.CalendarEventGroupResponse;
import com.bubli.personal.calendar.dto.CalendarGroupEventResponse;
import com.bubli.personal.calendar.dto.GoogleCalendarEventPayload;
import com.bubli.personal.calendar.dto.GoogleCalendarListEntry;
import com.bubli.personal.calendar.entity.GoogleCalendarConnection;
import com.bubli.personal.calendar.type.CalendarEventGroupType;
import com.bubli.project.dto.ProjectRoomResult;
import com.bubli.project.service.ProjectRoomPublicService;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.service.ScheduleCalendarPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoogleCalendarGroupService {

	private static final int DEFAULT_LOCAL_LIMIT = 200;
	private static final int MAX_LOCAL_LIMIT = 500;

	private final ScheduleCalendarPublicService scheduleCalendarPublicService;
	private final ProjectRoomPublicService projectRoomPublicService;
	private final GoogleCalendarConnectionService connectionService;
	private final GoogleCalendarClient googleCalendarClient;
	private final ProjectRoomCalendarService projectRoomCalendarService;

	@Transactional
	public List<CalendarEventGroupResponse> getGroupedEvents(
			UUID userId,
			UUID roomId,
			Instant from,
			Instant to,
			List<String> googleCalendarIds,
			Integer localLimit
	) {
		List<CalendarEventGroupResponse> groups = new ArrayList<>();
		boolean googleConnected = connectionService.hasActiveConnection(userId);
		List<CalendarEventGroupResponse> localGroups = groupLocalSchedules(
				userId,
				roomId,
				from,
				to,
				boundedLimit(localLimit),
				!googleConnected
		);
		groups.addAll(localGroups);
		if (googleConnected) {
			Set<String> excludedCalendarIds = new HashSet<>(projectRoomCalendarService.findManagedGoogleCalendarIds(userId));
			excludedCalendarIds.addAll(localRoomGoogleCalendarIds(localGroups));
			groups.addAll(groupGoogleEvents(userId, from, to, googleCalendarIds, excludedCalendarIds));
		}
		return groups.stream()
				.filter(group -> group.eventCount() > 0)
				.toList();
	}

	private List<CalendarEventGroupResponse> groupLocalSchedules(
			UUID userId,
			UUID roomId,
			Instant from,
			Instant to,
			int limit,
			boolean includeCachedGoogleGroups
	) {
		List<ScheduleResult> schedules = scheduleCalendarPublicService
				.getEvents(userId, roomId, from, to, PageRequest.of(0, limit))
				.getItems();
		Map<UUID, String> roomNames = findRoomNames(userId, schedules);
		List<CalendarGroupEventResponse> personalEvents = new ArrayList<>();
		Map<UUID, List<CalendarGroupEventResponse>> roomEvents = new LinkedHashMap<>();
		Map<String, List<CalendarGroupEventResponse>> cachedGoogleEvents = new LinkedHashMap<>();

		for (ScheduleResult schedule : schedules) {
			CalendarGroupEventResponse event = CalendarGroupEventResponse.fromSchedule(schedule);
			if (schedule.roomId() == null && schedule.googleEventId() == null) {
				personalEvents.add(event);
			} else if (schedule.roomId() != null) {
				roomEvents.computeIfAbsent(schedule.roomId(), ignored -> new ArrayList<>()).add(event);
			} else if (includeCachedGoogleGroups) {
				cachedGoogleEvents
						.computeIfAbsent(normalizeCalendarId(schedule.googleCalendarId()), ignored -> new ArrayList<>())
						.add(event);
			}
		}

		List<CalendarEventGroupResponse> groups = new ArrayList<>();
		groups.add(CalendarEventGroupResponse.of(
				CalendarEventGroupType.PERSONAL,
				"personal",
				"개인 일정",
				null,
				null,
				sortEvents(personalEvents)
		));
		Map<UUID, String> roomCalendarIds = projectRoomCalendarService.findGoogleCalendarIds(userId, roomEvents.keySet());
		for (Map.Entry<UUID, List<CalendarGroupEventResponse>> entry : roomEvents.entrySet()) {
			UUID projectRoomId = entry.getKey();
			groups.add(CalendarEventGroupResponse.of(
					CalendarEventGroupType.PROJECT_ROOM,
					projectRoomId.toString(),
					roomNames.getOrDefault(projectRoomId, projectRoomId.toString()),
					projectRoomId,
					roomCalendarIds.get(projectRoomId),
					sortEvents(entry.getValue())
			));
		}
		for (Map.Entry<String, List<CalendarGroupEventResponse>> entry : cachedGoogleEvents.entrySet()) {
			String googleCalendarId = entry.getKey();
			String groupName = entry.getValue().stream()
					.map(CalendarGroupEventResponse::googleCalendarSummary)
					.filter(summary -> summary != null && !summary.isBlank())
					.findFirst()
					.orElse(googleCalendarId);
			groups.add(CalendarEventGroupResponse.of(
					CalendarEventGroupType.GOOGLE_CALENDAR,
					googleCalendarId,
					groupName,
					null,
					googleCalendarId,
					sortEvents(entry.getValue())
			));
		}
		return groups;
	}

	private List<CalendarEventGroupResponse> groupGoogleEvents(
			UUID userId,
			Instant from,
			Instant to,
			List<String> googleCalendarIds,
			Set<String> excludedCalendarIds
	) {
		GoogleCalendarConnection connection = connectionService.getActiveConnectionWithFreshToken(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_404_001));
		List<GoogleCalendarListEntry> calendars = googleCalendarClient.getCalendars(connection.getAccessToken());
		List<String> selectedIds = normalizeCalendarIds(googleCalendarIds);
		if (!selectedIds.isEmpty()) {
			calendars = calendars.stream()
					.filter(calendar -> selectedIds.contains(calendar.id()))
					.toList();
		}
		if (!excludedCalendarIds.isEmpty()) {
			calendars = calendars.stream()
					.filter(calendar -> !excludedCalendarIds.contains(calendar.id()))
					.toList();
		}
		List<CalendarEventGroupResponse> groups = new ArrayList<>();
		for (GoogleCalendarListEntry calendar : calendars) {
			List<CalendarGroupEventResponse> events = googleCalendarClient
					.getEvents(connection.getAccessToken(), calendar.id(), from.toString(), to.toString())
					.stream()
					.filter(event -> !event.isCancelled())
					.filter(this::hasStartTime)
					.map(event -> CalendarGroupEventResponse.fromGoogleEvent(calendar.id(), calendar.displayName(), event))
					.toList();
			groups.add(CalendarEventGroupResponse.of(
					CalendarEventGroupType.GOOGLE_CALENDAR,
					calendar.id(),
					calendar.displayName(),
					null,
					calendar.id(),
					sortEvents(events)
			));
		}
		return groups;
	}

	private Set<String> localRoomGoogleCalendarIds(List<CalendarEventGroupResponse> localGroups) {
		return localGroups.stream()
				.filter(group -> group.groupType() == CalendarEventGroupType.PROJECT_ROOM)
				.map(CalendarEventGroupResponse::googleCalendarId)
				.filter(id -> id != null && !id.isBlank())
				.collect(Collectors.toSet());
	}

	private Map<UUID, String> findRoomNames(UUID userId, List<ScheduleResult> schedules) {
		Map<UUID, String> roomNames = new LinkedHashMap<>();
		for (ScheduleResult schedule : schedules) {
			if (schedule.roomId() == null || roomNames.containsKey(schedule.roomId())) {
				continue;
			}
			ProjectRoomResult room = projectRoomPublicService.getProjectRoom(userId, schedule.roomId());
			roomNames.put(room.id(), room.name());
		}
		return roomNames;
	}

	private List<CalendarGroupEventResponse> sortEvents(List<CalendarGroupEventResponse> events) {
		return events.stream()
				.sorted(Comparator.comparing(CalendarGroupEventResponse::startsAt))
				.toList();
	}

	private List<String> normalizeCalendarIds(List<String> calendarIds) {
		if (calendarIds == null || calendarIds.isEmpty()) {
			return List.of();
		}
		return calendarIds.stream()
				.filter(id -> id != null && !id.isBlank())
				.distinct()
				.toList();
	}

	private boolean hasStartTime(GoogleCalendarEventPayload event) {
		if (event.start() == null) {
			return false;
		}
		return event.start().dateTime() != null || event.start().date() != null;
	}

	private int boundedLimit(Integer localLimit) {
		if (localLimit == null || localLimit <= 0) {
			return DEFAULT_LOCAL_LIMIT;
		}
		return Math.min(localLimit, MAX_LOCAL_LIMIT);
	}

	private String normalizeCalendarId(String googleCalendarId) {
		return googleCalendarId == null || googleCalendarId.isBlank() ? "primary" : googleCalendarId;
	}
}
