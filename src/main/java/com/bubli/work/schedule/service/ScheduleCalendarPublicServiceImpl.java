package com.bubli.work.schedule.service;

import com.bubli.global.response.PageResponse;
import com.bubli.personal.calendar.dto.GoogleCalendarSyncResult;
import com.bubli.personal.calendar.service.GoogleCalendarScheduleSyncPublicService;
import com.bubli.work.schedule.dto.CreateScheduleCommand;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.dto.ScheduleSyncTarget;
import com.bubli.work.schedule.dto.UpdateScheduleCommand;
import com.bubli.work.schedule.entity.Schedule;
import com.bubli.work.schedule.repository.ScheduleRepository;
import com.bubli.work.schedule.type.ScheduleSyncStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleCalendarPublicServiceImpl implements ScheduleCalendarPublicService {

	private static final String UNTITLED_EVENT = "(제목 없음)";

	private final ScheduleService scheduleService;
	private final ScheduleRepository scheduleRepository;
	private final GoogleCalendarScheduleSyncPublicService googleCalendarScheduleSyncPublicService;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ScheduleResult> getEvents(
			UUID userId,
			UUID roomId,
			Instant from,
			Instant to,
			Pageable pageable
	) {
		return scheduleService.getSchedules(userId, roomId, from, to, pageable);
	}

	@Override
	@Transactional
	public ScheduleResult createEvent(UUID userId, CreateScheduleCommand command) {
		return scheduleService.create(userId, command);
	}

	@Override
	@Transactional
	public ScheduleResult updateEvent(UUID userId, UUID scheduleId, UpdateScheduleCommand command) {
		return scheduleService.update(userId, scheduleId, command);
	}

	@Override
	@Transactional
	public void deleteEvent(UUID userId, UUID scheduleId) {
		scheduleService.delete(userId, scheduleId);
	}

	@Override
	@Transactional
	public ScheduleResult upsertGoogleEvent(
			UUID userId,
			String googleEventId,
			String title,
			Instant startsAt,
			Instant endsAt
	) {
		Schedule schedule = scheduleRepository.findByOwnerUserIdAndGoogleEventId(userId, googleEventId)
				.orElseGet(() -> Schedule.create(
						userId,
						null,
						null,
						null,
						normalizeTitle(title),
						startsAt,
						endsAt,
						false
				));
		schedule.update(normalizeTitle(title), startsAt, endsAt, false, null, null);
		schedule.markSynced(googleEventId);
		return ScheduleResult.from(scheduleRepository.save(schedule));
	}

	@Override
	@Transactional
	public List<ScheduleResult> pushUnsyncedEvents(UUID userId, Instant from, Instant to) {
		return scheduleRepository.findByOwnerUserIdAndSyncStatusInAndStartsAtBetweenOrderByStartsAtAsc(
						userId,
						List.of(ScheduleSyncStatus.LOCAL_ONLY, ScheduleSyncStatus.SYNC_FAILED),
						from,
						to
				)
				.stream()
				.map(schedule -> {
					GoogleCalendarSyncResult syncResult = googleCalendarScheduleSyncPublicService
							.syncCreatedOrUpdatedSchedule(userId, ScheduleSyncTarget.from(schedule));
					markGoogleSyncResult(schedule, syncResult);
					return ScheduleResult.from(schedule);
				})
				.toList();
	}

	private void markGoogleSyncResult(Schedule schedule, GoogleCalendarSyncResult syncResult) {
		if (syncResult == null || !syncResult.attempted()) {
			return;
		}
		if (!syncResult.succeeded()) {
			schedule.markSyncFailed();
			return;
		}
		schedule.markSynced(syncResult.googleEventId());
	}

	private String normalizeTitle(String title) {
		return title == null || title.isBlank() ? UNTITLED_EVENT : title.trim();
	}
}
