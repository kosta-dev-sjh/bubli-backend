package com.bubli.personal.timer.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.personal.timer.dto.StartTimeLogCommand;
import com.bubli.personal.timer.dto.TimeLogResponse;
import com.bubli.personal.timer.entity.TimeLog;
import com.bubli.personal.timer.repository.TimeLogRepository;
import com.bubli.personal.timer.type.TimeLogStatus;
import com.bubli.personal.timer.type.TimerType;
import com.bubli.project.service.ProjectMembershipPublicService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TimeLogServiceTest {

	@Mock
	TimeLogRepository timeLogRepository;

	@Mock
	ProjectMembershipPublicService projectMembershipPublicService;

	@InjectMocks
	TimeLogService timeLogService;

	@Test
	void startCreatesGeneralTimerWhenIdempotencyKeyIsNew() {
		UUID userId = UUID.randomUUID();
		UUID timeLogId = UUID.randomUUID();
		TimeLog created = TimeLog.start(userId, null, TimerType.GENERAL, "timer-key-1", null, Instant.now());
		ReflectionTestUtils.setField(created, "id", timeLogId);
		given(timeLogRepository.findByIdempotencyKey("timer-key-1"))
				.willReturn(Optional.empty(), Optional.of(created));
		given(timeLogRepository.insertRunningIfNoConflict(
				any(UUID.class),
				eq(userId),
				nullable(UUID.class),
				eq(TimerType.GENERAL.name()),
				eq("timer-key-1"),
				nullable(UUID.class),
				any(Instant.class)
		)).willReturn(1);

		TimeLogResponse result = timeLogService.start(new StartTimeLogCommand(
				userId,
				null,
				TimerType.GENERAL,
				"timer-key-1",
				null
		));

		assertThat(result.id()).isEqualTo(timeLogId);
		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.timerType()).isEqualTo(TimerType.GENERAL);
		assertThat(result.status()).isEqualTo(TimeLogStatus.RUNNING);
		assertThat(result.durationSeconds()).isZero();
		verify(timeLogRepository).insertRunningIfNoConflict(
				any(UUID.class),
				eq(userId),
				nullable(UUID.class),
				eq(TimerType.GENERAL.name()),
				eq("timer-key-1"),
				nullable(UUID.class),
				any(Instant.class)
		);
	}

	@Test
	void startThrowsWhenUserAlreadyHasRunningTimer() {
		UUID userId = UUID.randomUUID();
		given(timeLogRepository.findByIdempotencyKey("timer-key-running"))
				.willReturn(Optional.empty(), Optional.empty());
		given(timeLogRepository.insertRunningIfNoConflict(
				any(UUID.class),
				eq(userId),
				nullable(UUID.class),
				eq(TimerType.GENERAL.name()),
				eq("timer-key-running"),
				nullable(UUID.class),
				any(Instant.class)
		)).willReturn(0);

		assertThatThrownBy(() -> timeLogService.start(new StartTimeLogCommand(
				userId,
				null,
				TimerType.GENERAL,
				"timer-key-running",
				null
		)))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PERSONAL_409_001);
		verify(timeLogRepository).insertRunningIfNoConflict(
				any(UUID.class),
				eq(userId),
				nullable(UUID.class),
				eq(TimerType.GENERAL.name()),
				eq("timer-key-running"),
				nullable(UUID.class),
				any(Instant.class)
		);
	}

	@Test
	void startReturnsExistingTimerForSameIdempotencyKeyAndUser() {
		UUID userId = UUID.randomUUID();
		UUID timeLogId = UUID.randomUUID();
		TimeLog existing = TimeLog.start(userId, null, TimerType.GENERAL, "timer-key-2", null, Instant.now());
		ReflectionTestUtils.setField(existing, "id", timeLogId);
		given(timeLogRepository.findByIdempotencyKey("timer-key-2")).willReturn(Optional.of(existing));

		TimeLogResponse result = timeLogService.start(new StartTimeLogCommand(
				userId,
				null,
				TimerType.GENERAL,
				"timer-key-2",
				null
		));

		assertThat(result.id()).isEqualTo(timeLogId);
		verify(timeLogRepository, never()).insertRunningIfNoConflict(
				any(UUID.class),
				any(UUID.class),
				nullable(UUID.class),
				any(String.class),
				any(String.class),
				nullable(UUID.class),
				any(Instant.class)
		);
	}

	@Test
	void pauseSucceedsWhenRunning() {
		UUID userId = UUID.randomUUID();
		UUID timeLogId = UUID.randomUUID();
		TimeLog timeLog = TimeLog.start(userId, null, TimerType.GENERAL, "timer-key-pause", null, Instant.now());
		ReflectionTestUtils.setField(timeLog, "id", timeLogId);
		ReflectionTestUtils.setField(timeLog, "lastStartedAt", Instant.now().minusSeconds(5));
		given(timeLogRepository.findByIdAndUserId(timeLogId, userId)).willReturn(Optional.of(timeLog));

		TimeLogResponse result = timeLogService.pause(userId, timeLogId);

		assertThat(result.status()).isEqualTo(TimeLogStatus.PAUSED);
		assertThat(result.durationSeconds()).isGreaterThanOrEqualTo(5);
	}

	@Test
	void resumeSucceedsWhenPaused() {
		UUID userId = UUID.randomUUID();
		UUID timeLogId = UUID.randomUUID();
		TimeLog timeLog = TimeLog.start(userId, null, TimerType.GENERAL, "timer-key-resume", null, Instant.now());
		ReflectionTestUtils.setField(timeLog, "id", timeLogId);
		timeLog.pause(Instant.now());
		given(timeLogRepository.findByIdAndUserId(timeLogId, userId)).willReturn(Optional.of(timeLog));
		given(timeLogRepository.findFirstByUserIdAndStatus(userId, TimeLogStatus.RUNNING)).willReturn(Optional.empty());

		TimeLogResponse result = timeLogService.resume(userId, timeLogId);

		assertThat(result.status()).isEqualTo(TimeLogStatus.RUNNING);
	}

	@Test
	void resumeRejectsWhenAnotherTimerIsAlreadyRunning() {
		UUID userId = UUID.randomUUID();
		UUID pausedTimeLogId = UUID.randomUUID();
		TimeLog paused = TimeLog.start(userId, null, TimerType.GENERAL, "timer-key-paused", null, Instant.now());
		ReflectionTestUtils.setField(paused, "id", pausedTimeLogId);
		paused.pause(Instant.now());
		TimeLog running = TimeLog.start(userId, null, TimerType.GENERAL, "timer-key-running-other", null, Instant.now());
		ReflectionTestUtils.setField(running, "id", UUID.randomUUID());
		given(timeLogRepository.findByIdAndUserId(pausedTimeLogId, userId)).willReturn(Optional.of(paused));
		given(timeLogRepository.findFirstByUserIdAndStatus(userId, TimeLogStatus.RUNNING))
				.willReturn(Optional.of(running));

		assertThatThrownBy(() -> timeLogService.resume(userId, pausedTimeLogId))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PERSONAL_409_001);

		assertThat(paused.getStatus()).isEqualTo(TimeLogStatus.PAUSED);
	}

	@Test
	void stopSucceedsWhenRunningAndCalculatesDuration() {
		UUID userId = UUID.randomUUID();
		UUID timeLogId = UUID.randomUUID();
		TimeLog timeLog = TimeLog.start(userId, null, TimerType.GENERAL, "timer-key-stop", null, Instant.now());
		ReflectionTestUtils.setField(timeLog, "id", timeLogId);
		ReflectionTestUtils.setField(timeLog, "lastStartedAt", Instant.now().minusSeconds(100));
		given(timeLogRepository.findByIdAndUserId(timeLogId, userId)).willReturn(Optional.of(timeLog));

		TimeLogResponse result = timeLogService.stop(userId, timeLogId);

		assertThat(result.status()).isEqualTo(TimeLogStatus.ENDED);
		assertThat(result.endedAt()).isNotNull();
		assertThat(result.durationSeconds()).isGreaterThanOrEqualTo(100);
	}

	@Test
	void throwsWhenAccessingAnotherUsersTimer() {
		UUID ownerId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		UUID timeLogId = UUID.randomUUID();
		given(timeLogRepository.findByIdAndUserId(timeLogId, otherUserId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> timeLogService.pause(otherUserId, timeLogId))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PERSONAL_404_001);
	}

	@Test
	void startWorkTimerRequiresRoomMembership() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		given(timeLogRepository.findByIdempotencyKey("timer-key-3")).willReturn(Optional.empty());
		willThrow(BusinessException.class)
				.given(projectMembershipPublicService)
				.assertActiveMember(userId, roomId);

		assertThatThrownBy(() -> timeLogService.start(new StartTimeLogCommand(
				userId,
				roomId,
				TimerType.WORK,
				"timer-key-3",
				null
		))).isInstanceOf(BusinessException.class);
		verify(timeLogRepository, never()).insertRunningIfNoConflict(
				any(UUID.class),
				any(UUID.class),
				nullable(UUID.class),
				any(String.class),
				any(String.class),
				nullable(UUID.class),
				any(Instant.class)
		);
	}

	@Test
	void heartbeatRejectsPausedTimer() {
		UUID userId = UUID.randomUUID();
		UUID timeLogId = UUID.randomUUID();
		TimeLog timeLog = TimeLog.start(userId, null, TimerType.GENERAL, "timer-key-5", null, Instant.now());
		ReflectionTestUtils.setField(timeLog, "id", timeLogId);
		timeLog.pause(Instant.now());
		given(timeLogRepository.findByIdAndUserId(timeLogId, userId)).willReturn(Optional.of(timeLog));

		assertThatThrownBy(() -> timeLogService.heartbeat(userId, timeLogId))
				.isInstanceOf(BusinessException.class);
	}
}
