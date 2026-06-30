package com.bubli.work.schedule.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.work.schedule.dto.CreateScheduleCommand;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.entity.Schedule;
import com.bubli.work.schedule.repository.ScheduleRepository;
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

	@Override
	@Transactional(readOnly = true)
	public List<ScheduleResult> getSchedulesBetween(UUID userId, Instant from, Instant to) {
		return scheduleRepository.findByOwnerUserIdAndStartsAtBetweenOrderByStartsAtAsc(userId, from, to)
				.stream()
				.map(ScheduleResult::from)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<ScheduleResult> getRoomSchedulesBetween(UUID roomId, Instant from, Instant to) {
		return scheduleRepository.findByRoomIdAndStartsAtBetweenOrderByStartsAtAsc(roomId, from, to)
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
		return ScheduleResult.from(scheduleRepository.save(schedule));
	}

	private void validateRange(Instant startsAt, Instant endsAt) {
		if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
			throw new BusinessException(ErrorCode.SCHEDULE_400_001);
		}
	}
}
