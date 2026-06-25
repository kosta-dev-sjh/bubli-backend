package com.bubli.work.schedule.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.project.service.RoomAccessService;
import com.bubli.work.schedule.dto.CreateScheduleCommand;
import com.bubli.work.schedule.dto.UpdateScheduleCommand;
import com.bubli.work.schedule.entity.Schedule;
import com.bubli.work.schedule.repository.ScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

	@Mock
	ScheduleRepository scheduleRepository;

	@Mock
	RoomAccessService roomAccessService;

	@InjectMocks
	ScheduleService scheduleService;

	@Test
	void createPersonalScheduleSavesOwnerSchedule() {
		UUID userId = UUID.randomUUID();
		Instant startsAt = Instant.parse("2026-07-02T01:00:00Z");
		Instant endsAt = Instant.parse("2026-07-02T02:00:00Z");
		given(scheduleRepository.save(any(Schedule.class))).willAnswer(invocation -> {
			Schedule schedule = invocation.getArgument(0);
			ReflectionTestUtils.setField(schedule, "id", UUID.randomUUID());
			return schedule;
		});

		Schedule result = scheduleService.create(userId, new CreateScheduleCommand(
				null,
				null,
				null,
				"개인 포트폴리오 정리",
				startsAt,
				endsAt,
				false
		));

		assertThat(result.getOwnerUserId()).isEqualTo(userId);
		assertThat(result.getRoomId()).isNull();
		assertThat(result.getTitle()).isEqualTo("개인 포트폴리오 정리");
		assertThat(result.getStartsAt()).isEqualTo(startsAt);

		ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
		verify(scheduleRepository).save(scheduleCaptor.capture());
		assertThat(scheduleCaptor.getValue().getOwnerUserId()).isEqualTo(userId);
	}

	@Test
	void createRoomScheduleRequiresActiveRoomMember() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		willThrow(new BusinessException(ErrorCode.PROJECT_403_001))
				.given(roomAccessService)
				.validateActiveMember(userId, roomId);

		assertThatThrownBy(() -> scheduleService.create(userId, new CreateScheduleCommand(
				roomId,
				null,
				null,
				"클라이언트 중간 리뷰",
				Instant.parse("2026-07-03T05:00:00Z"),
				Instant.parse("2026-07-03T06:00:00Z"),
				false
		))).isInstanceOf(BusinessException.class);
	}

	@Test
	void getSchedulesReturnsPersonalAndAccessibleRoomSchedules() {
		UUID userId = UUID.randomUUID();
		Schedule personalSchedule = Schedule.create(
				userId,
				null,
				null,
				null,
				"개인 일정",
				Instant.parse("2026-07-02T01:00:00Z"),
				null,
				false
		);
		PageRequest pageable = PageRequest.of(0, 20);
		given(roomAccessService.findActiveRoomIds(userId)).willReturn(List.of());
		given(scheduleRepository.findAll(anyScheduleSpec(), anyPageable()))
				.willReturn(new org.springframework.data.domain.PageImpl<>(List.of(personalSchedule), pageable, 1));

		PageResponse<Schedule> result = scheduleService.getSchedules(userId, null, null, null, pageable);

		assertThat(result.getItems()).containsExactly(personalSchedule);
		assertThat(result.getTotalElements()).isEqualTo(1);
	}

	@Test
	void updatePersonalScheduleRejectsOtherUser() {
		UUID ownerId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		UUID scheduleId = UUID.randomUUID();
		Schedule schedule = Schedule.create(
				ownerId,
				null,
				null,
				null,
				"개인 일정",
				Instant.parse("2026-07-02T01:00:00Z"),
				null,
				false
		);
		ReflectionTestUtils.setField(schedule, "id", scheduleId);
		given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

		assertThatThrownBy(() -> scheduleService.update(otherUserId, scheduleId, new UpdateScheduleCommand(
				"수정",
				null,
				null,
				null,
				null,
				null
		))).isInstanceOf(BusinessException.class);
	}

	@Test
	void updateRejectsInvalidTimeRange() {
		UUID userId = UUID.randomUUID();
		UUID scheduleId = UUID.randomUUID();
		Schedule schedule = Schedule.create(
				userId,
				null,
				null,
				null,
				"개인 일정",
				Instant.parse("2026-07-02T01:00:00Z"),
				null,
				false
		);
		ReflectionTestUtils.setField(schedule, "id", scheduleId);
		given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

		assertThatThrownBy(() -> scheduleService.update(userId, scheduleId, new UpdateScheduleCommand(
				null,
				Instant.parse("2026-07-02T03:00:00Z"),
				Instant.parse("2026-07-02T02:00:00Z"),
				null,
				null,
				null
		))).isInstanceOf(BusinessException.class);
	}

	private Specification<Schedule> anyScheduleSpec() {
		return org.mockito.ArgumentMatchers.<Specification<Schedule>>any();
	}

	private Pageable anyPageable() {
		return org.mockito.ArgumentMatchers.<Pageable>any();
	}
}
