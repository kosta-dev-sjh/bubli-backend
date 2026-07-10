package com.bubli.work.schedule.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.personal.calendar.dto.GoogleCalendarSyncResult;
import com.bubli.personal.calendar.service.GoogleCalendarScheduleSyncPublicService;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.work.schedule.dto.CreateScheduleCommand;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.dto.ScheduleSyncTarget;
import com.bubli.work.schedule.entity.Schedule;
import com.bubli.work.schedule.repository.ScheduleRepository;
import com.bubli.work.task.service.TaskPublicService;
import com.bubli.work.wbs.service.WbsItemPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchedulePublicServiceImpl implements SchedulePublicService {

	private final ScheduleRepository scheduleRepository;
	private final ProjectMembershipPublicService projectMembershipPublicService;
	private final TaskPublicService taskPublicService;
	private final WbsItemPublicService wbsItemPublicService;
	private final GoogleCalendarScheduleSyncPublicService googleCalendarScheduleSyncPublicService;

	@Override
	@Transactional(readOnly = true)
	public List<ScheduleResult> getSchedulesBetween(UUID userId, Instant from, Instant to) {
		List<UUID> activeRoomIds = projectMembershipPublicService.findActiveRoomIds(userId);
		List<Schedule> schedules = activeRoomIds.isEmpty()
				? scheduleRepository.findPersonalBetweenForUser(userId, from, to)
				: scheduleRepository.findVisibleBetweenForUser(userId, activeRoomIds, from, to);
		return schedules
				.stream()
				.map(ScheduleResult::from)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<ScheduleResult> getRoomSchedulesBetween(UUID roomId, Instant from, Instant to) {
		return scheduleRepository.findRoomOverlappingForRoom(roomId, from, to)
				.stream()
				.map(ScheduleResult::from)
				.toList();
	}

	@Override
	@Transactional
	public ScheduleResult create(UUID userId, CreateScheduleCommand command) {
		validateRange(command.startsAt(), command.endsAt());
		if (command.roomId() != null) {
			projectMembershipPublicService.assertActiveMember(userId, command.roomId());
		}
		validateLinkedWorkScope(userId, command.roomId(), command.taskId(), command.wbsItemId());
		Schedule schedule = Schedule.create(
				userId,
				command.roomId(),
				command.taskId(),
				command.wbsItemId(),
				command.title(),
				command.startsAt(),
				command.endsAt(),
				command.allDay()
		);
		Schedule savedSchedule = scheduleRepository.save(schedule);
		GoogleCalendarSyncResult syncResult = syncCreatedOrUpdatedSchedule(userId, ScheduleSyncTarget.from(savedSchedule));
		if (syncResult == null) {
			return ScheduleResult.from(savedSchedule);
		}
		if (!syncResult.attempted()) {
			return ScheduleResult.from(savedSchedule);
		}
		if (!syncResult.succeeded()) {
			savedSchedule.markSyncFailed();
		} else {
			savedSchedule.markSynced(
					syncResult.googleCalendarId(),
					syncResult.googleCalendarSummary(),
					syncResult.googleEventId()
			);
		}
		return ScheduleResult.from(savedSchedule);
	}

	@Override
	@Transactional(readOnly = true)
	public void assertNoScheduleLinkedToWbsItem(UUID wbsItemId) {
		if (scheduleRepository.existsByWbsItemId(wbsItemId)) {
			throw new BusinessException(ErrorCode.WORK_400_003);
		}
	}

	@Override
	@Transactional
	public void deleteSchedulesLinkedToWbsItem(UUID wbsItemId) {
		List<Schedule> schedules = scheduleRepository.findByWbsItemId(wbsItemId);
		for (Schedule schedule : schedules) {
			deleteSyncedSchedule(schedule);
		}
		scheduleRepository.deleteAll(schedules);
	}

	@Override
	@Transactional(readOnly = true)
	public void assertNoScheduleLinkedToTask(UUID taskId) {
		if (scheduleRepository.existsByTaskId(taskId)) {
			throw new BusinessException(ErrorCode.WORK_400_004);
		}
	}

	private void validateLinkedWorkScope(UUID userId, UUID roomId, UUID taskId, UUID wbsItemId) {
		if (wbsItemId != null) {
			if (roomId == null) {
				throw new BusinessException(ErrorCode.SCHEDULE_400_001);
			}
			wbsItemPublicService.assertRoomWbsItem(roomId, wbsItemId);
		}
		taskPublicService.assertScheduleTaskScope(userId, roomId, taskId);
	}

	private GoogleCalendarSyncResult syncCreatedOrUpdatedSchedule(UUID userId, ScheduleSyncTarget schedule) {
		try {
			return googleCalendarScheduleSyncPublicService.syncCreatedOrUpdatedSchedule(userId, schedule);
		} catch (RuntimeException exception) {
			return GoogleCalendarSyncResult.failed();
		}
	}

	private void deleteSyncedSchedule(Schedule schedule) {
		try {
			googleCalendarScheduleSyncPublicService.deleteSyncedSchedule(
					schedule.getOwnerUserId(),
					ScheduleSyncTarget.from(schedule)
			);
		} catch (RuntimeException exception) {
			// 외부 캘린더 삭제 실패가 Bubli WBS 삭제를 막지 않게 한다.
		}
	}

	private void validateRange(Instant startsAt, Instant endsAt) {
		if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
			throw new BusinessException(ErrorCode.SCHEDULE_400_001);
		}
	}
}
