package com.bubli.work.schedule.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.personal.calendar.dto.GoogleCalendarSyncResult;
import com.bubli.personal.calendar.service.GoogleCalendarScheduleSyncPublicService;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.work.task.service.TaskPublicService;
import com.bubli.work.schedule.dto.CreateScheduleCommand;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.dto.ScheduleSyncTarget;
import com.bubli.work.schedule.dto.UpdateScheduleCommand;
import com.bubli.work.schedule.entity.Schedule;
import com.bubli.work.schedule.repository.ScheduleRepository;
import com.bubli.work.wbs.service.WbsItemPublicService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleService {

	private final ScheduleRepository scheduleRepository;
	private final TaskPublicService taskPublicService;
	private final WbsItemPublicService wbsItemPublicService;
	private final ProjectMembershipPublicService projectMembershipPublicService;
	private final GoogleCalendarScheduleSyncPublicService googleCalendarScheduleSyncPublicService;

	@Transactional(readOnly = true)
	public PageResponse<ScheduleResult> getSchedules(UUID userId, UUID roomId, Instant from, Instant to, Pageable pageable) {
		validateRange(from, to);
		if (roomId != null) {
			projectMembershipPublicService.assertActiveMember(userId, roomId);
		}
		Page<Schedule> page = scheduleRepository.findAll(
				visibleScheduleSpec(userId, roomId, from, to),
				withDefaultSort(pageable)
		);
		return new PageResponse<>(
				page.getContent().stream()
						.map(ScheduleResult::from)
						.toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext()
		);
	}

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
		markGoogleSyncResult(
				savedSchedule,
				syncCreatedOrUpdatedSchedule(
						userId,
						ScheduleSyncTarget.from(savedSchedule)
				)
		);
		return ScheduleResult.from(savedSchedule);
	}

	@Transactional
	public ScheduleResult update(UUID userId, UUID scheduleId, UpdateScheduleCommand command) {
		Schedule schedule = getAccessibleSchedule(userId, scheduleId);
		Instant startsAt = command.startsAt() == null ? schedule.getStartsAt() : command.startsAt();
		validateRange(startsAt, command.endsAt());
		if (command.title() != null && command.title().isBlank()) {
			throw new BusinessException(ErrorCode.SCHEDULE_400_001);
		}
		validateLinkedWorkScope(userId, schedule.getRoomId(), command.taskId(), command.wbsItemId());
		schedule.update(
				command.title(),
				command.startsAt(),
				command.endsAt(),
				command.allDay(),
				command.taskId(),
				command.wbsItemId()
		);
		markGoogleSyncResult(
				schedule,
				syncCreatedOrUpdatedSchedule(
						syncUserIdFor(schedule, userId),
						ScheduleSyncTarget.from(schedule)
				)
		);
		return ScheduleResult.from(schedule);
	}

	@Transactional
	public void delete(UUID userId, UUID scheduleId) {
		Schedule schedule = getAccessibleSchedule(userId, scheduleId);
		deleteSyncedSchedule(
				syncUserIdFor(schedule, userId),
				ScheduleSyncTarget.from(schedule)
		);
		scheduleRepository.delete(schedule);
	}

	private void markGoogleSyncResult(Schedule schedule, GoogleCalendarSyncResult syncResult) {
		if (syncResult == null) {
			return;
		}
		if (!syncResult.attempted()) {
			return;
		}
		if (!syncResult.succeeded()) {
			schedule.markSyncFailed();
			return;
		}
		schedule.markSynced(
				syncResult.googleCalendarId(),
				syncResult.googleCalendarSummary(),
				syncResult.googleEventId()
		);
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

	private void deleteSyncedSchedule(UUID userId, ScheduleSyncTarget schedule) {
		try {
			googleCalendarScheduleSyncPublicService.deleteSyncedSchedule(userId, schedule);
		} catch (RuntimeException exception) {
			// 외부 캘린더 삭제 실패가 Bubli 일정 삭제를 막지 않게 한다.
		}
	}

	private Schedule getAccessibleSchedule(UUID userId, UUID scheduleId) {
		Schedule schedule = scheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_404_001));
		checkScheduleAccess(userId, schedule);
		return schedule;
	}

	private UUID syncUserIdFor(Schedule schedule, UUID actorUserId) {
		if (schedule.getRoomId() != null) {
			return schedule.getOwnerUserId();
		}
		return actorUserId;
	}

	private void checkScheduleAccess(UUID userId, Schedule schedule) {
		if (schedule.getRoomId() == null) {
			if (!schedule.getOwnerUserId().equals(userId)) {
				throw new BusinessException(ErrorCode.SCHEDULE_403_001);
			}
			return;
		}
		projectMembershipPublicService.assertActiveMember(userId, schedule.getRoomId());
	}

	private void validateRange(Instant startsAt, Instant endsAt) {
		if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
			throw new BusinessException(ErrorCode.SCHEDULE_400_001);
		}
	}

	private Specification<Schedule> visibleScheduleSpec(UUID userId, UUID roomId, Instant from, Instant to) {
		List<UUID> activeRoomIds = roomId == null ? projectMembershipPublicService.findActiveRoomIds(userId) : List.of();
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (roomId == null) {
				Predicate personalSchedule = criteriaBuilder.and(
						criteriaBuilder.isNull(root.get("roomId")),
						criteriaBuilder.equal(root.get("ownerUserId"), userId)
				);
				if (activeRoomIds.isEmpty()) {
					predicates.add(personalSchedule);
				} else {
					predicates.add(criteriaBuilder.or(personalSchedule, root.get("roomId").in(activeRoomIds)));
				}
			} else {
				predicates.add(criteriaBuilder.equal(root.get("roomId"), roomId));
			}
			if (from != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startsAt"), from));
			}
			if (to != null) {
				predicates.add(criteriaBuilder.lessThan(root.get("startsAt"), to));
			}
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private Pageable withDefaultSort(Pageable pageable) {
		if (pageable.getSort().isSorted()) {
			return pageable;
		}
		return PageRequest.of(
				pageable.getPageNumber(),
				pageable.getPageSize(),
				Sort.by("startsAt").ascending().and(Sort.by("createdAt").ascending())
		);
	}
}
