package com.bubli.personal.timer.service;

import com.bubli.global.error.BusinessException;
import com.bubli.personal.timer.dto.StartTimeLogCommand;
import com.bubli.personal.timer.dto.TimeLogResult;
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
		given(timeLogRepository.findByIdempotencyKey("timer-key-1")).willReturn(Optional.empty());
		given(timeLogRepository.save(any(TimeLog.class))).willAnswer(invocation -> {
			TimeLog timeLog = invocation.getArgument(0);
			ReflectionTestUtils.setField(timeLog, "id", timeLogId);
			return timeLog;
		});

		TimeLogResult result = timeLogService.start(new StartTimeLogCommand(
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
	}

	@Test
	void startReturnsExistingTimerForSameIdempotencyKeyAndUser() {
		UUID userId = UUID.randomUUID();
		UUID timeLogId = UUID.randomUUID();
		TimeLog existing = TimeLog.start(userId, null, TimerType.GENERAL, "timer-key-2", null, Instant.now());
		ReflectionTestUtils.setField(existing, "id", timeLogId);
		given(timeLogRepository.findByIdempotencyKey("timer-key-2")).willReturn(Optional.of(existing));

		TimeLogResult result = timeLogService.start(new StartTimeLogCommand(
				userId,
				null,
				TimerType.GENERAL,
				"timer-key-2",
				null
		));

		assertThat(result.id()).isEqualTo(timeLogId);
		verify(timeLogRepository, never()).save(any(TimeLog.class));
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
		verify(timeLogRepository, never()).save(any(TimeLog.class));
	}

	@Test
	void pauseResumeStopChangesTimerState() {
		UUID userId = UUID.randomUUID();
		UUID timeLogId = UUID.randomUUID();
		TimeLog timeLog = TimeLog.start(userId, null, TimerType.GENERAL, "timer-key-4", null, Instant.now());
		ReflectionTestUtils.setField(timeLog, "id", timeLogId);
		ReflectionTestUtils.setField(timeLog, "lastStartedAt", Instant.now().minusSeconds(5));
		given(timeLogRepository.findById(timeLogId)).willReturn(Optional.of(timeLog));

		TimeLogResult paused = timeLogService.pause(userId, timeLogId);
		assertThat(paused.status()).isEqualTo(TimeLogStatus.PAUSED);
		assertThat(paused.durationSeconds()).isGreaterThanOrEqualTo(5);

		TimeLogResult resumed = timeLogService.resume(userId, timeLogId);
		assertThat(resumed.status()).isEqualTo(TimeLogStatus.RUNNING);

		TimeLogResult stopped = timeLogService.stop(userId, timeLogId);
		assertThat(stopped.status()).isEqualTo(TimeLogStatus.ENDED);
		assertThat(stopped.endedAt()).isNotNull();
	}

	@Test
	void heartbeatRejectsPausedTimer() {
		UUID userId = UUID.randomUUID();
		UUID timeLogId = UUID.randomUUID();
		TimeLog timeLog = TimeLog.start(userId, null, TimerType.GENERAL, "timer-key-5", null, Instant.now());
		ReflectionTestUtils.setField(timeLog, "id", timeLogId);
		timeLog.pause(Instant.now());
		given(timeLogRepository.findById(timeLogId)).willReturn(Optional.of(timeLog));

		assertThatThrownBy(() -> timeLogService.heartbeat(userId, timeLogId))
				.isInstanceOf(BusinessException.class);
	}
}
