package com.bubli.work.schedule.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.project.entity.RoomMember;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.RoomMemberStatus;
import com.bubli.work.schedule.dto.CreateScheduleCommand;
import com.bubli.work.schedule.dto.UpdateScheduleCommand;
import com.bubli.work.schedule.entity.Schedule;
import com.bubli.work.schedule.repository.ScheduleRepository;
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
	private final RoomMemberRepository roomMemberRepository;

	@Transactional(readOnly = true)
	public PageResponse<Schedule> getSchedules(UUID userId, UUID roomId, Instant from, Instant to, Pageable pageable) {
		validateRange(from, to);
		if (roomId != null) {
			checkActiveMember(userId, roomId);
		}
		Page<Schedule> page = scheduleRepository.findAll(
				visibleScheduleSpec(userId, roomId, from, to),
				withDefaultSort(pageable)
		);
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext()
		);
	}

	@Transactional
	public Schedule create(UUID userId, CreateScheduleCommand command) {
		validateRange(command.startsAt(), command.endsAt());
		if (command.roomId() != null) {
			checkActiveMember(userId, command.roomId());
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
		return scheduleRepository.save(schedule);
	}

	@Transactional
	public Schedule update(UUID userId, UUID scheduleId, UpdateScheduleCommand command) {
		Schedule schedule = getAccessibleSchedule(userId, scheduleId);
		Instant startsAt = command.startsAt() == null ? schedule.getStartsAt() : command.startsAt();
		validateRange(startsAt, command.endsAt());
		if (command.title() != null && command.title().isBlank()) {
			throw new BusinessException(ErrorCode.SCHEDULE_400_001);
		}
		schedule.update(
				command.title(),
				command.startsAt(),
				command.endsAt(),
				command.allDay(),
				command.taskId(),
				command.wbsItemId()
		);
		return schedule;
	}

	@Transactional
	public void delete(UUID userId, UUID scheduleId) {
		Schedule schedule = getAccessibleSchedule(userId, scheduleId);
		scheduleRepository.delete(schedule);
	}

	private Schedule getAccessibleSchedule(UUID userId, UUID scheduleId) {
		Schedule schedule = scheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_404_001));
		checkScheduleAccess(userId, schedule);
		return schedule;
	}

	private void checkScheduleAccess(UUID userId, Schedule schedule) {
		if (schedule.getRoomId() == null) {
			if (!schedule.getOwnerUserId().equals(userId)) {
				throw new BusinessException(ErrorCode.SCHEDULE_403_001);
			}
			return;
		}
		checkActiveMember(userId, schedule.getRoomId());
	}

	private void checkActiveMember(UUID userId, UUID roomId) {
		boolean activeMember = roomMemberRepository.existsByRoomIdAndUserIdAndStatus(
				roomId,
				userId,
				RoomMemberStatus.ACTIVE
		);
		if (!activeMember) {
			throw new BusinessException(ErrorCode.PROJECT_403_001);
		}
	}

	private void validateRange(Instant startsAt, Instant endsAt) {
		if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
			throw new BusinessException(ErrorCode.SCHEDULE_400_001);
		}
	}

	private Specification<Schedule> visibleScheduleSpec(UUID userId, UUID roomId, Instant from, Instant to) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (roomId == null) {
				Predicate personalSchedule = criteriaBuilder.and(
						criteriaBuilder.isNull(root.get("roomId")),
						criteriaBuilder.equal(root.get("ownerUserId"), userId)
				);
				predicates.add(criteriaBuilder.or(personalSchedule, criteriaBuilder.exists(activeMemberSubquery(
						userId,
						root.get("roomId"),
						query,
						criteriaBuilder
				))));
			} else {
				predicates.add(criteriaBuilder.equal(root.get("roomId"), roomId));
				predicates.add(criteriaBuilder.exists(activeMemberSubquery(
						userId,
						root.get("roomId"),
						query,
						criteriaBuilder
				)));
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

	private jakarta.persistence.criteria.Subquery<Integer> activeMemberSubquery(UUID userId,
			jakarta.persistence.criteria.Path<UUID> roomIdPath,
			jakarta.persistence.criteria.CriteriaQuery<?> query,
			jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder) {
		jakarta.persistence.criteria.Subquery<Integer> subquery = query.subquery(Integer.class);
		var roomMember = subquery.from(RoomMember.class);
		subquery.select(criteriaBuilder.literal(1));
		subquery.where(
				criteriaBuilder.equal(roomMember.get("roomId"), roomIdPath),
				criteriaBuilder.equal(roomMember.get("userId"), userId),
				criteriaBuilder.equal(roomMember.get("status"), RoomMemberStatus.ACTIVE)
		);
		return subquery;
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
