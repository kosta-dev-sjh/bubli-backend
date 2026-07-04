package com.bubli.personal.timer.service;

import com.bubli.personal.timer.entity.TimeLog;
import com.bubli.personal.timer.repository.TimeLogRepository;
import com.bubli.personal.timer.type.TimeLogStatus;
import com.bubli.personal.timer.type.TimerType;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TimeLogPublicServiceImplTest {

	@Mock
	TimeLogRepository timeLogRepository;

	@InjectMocks
	TimeLogPublicServiceImpl timeLogPublicService;

	@Test
	void stopRunningRoomTimerEndsRunningRoomTimer() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		TimeLog timeLog = TimeLog.start(userId, roomId, TimerType.WORK, "timer-key-room", null, Instant.now());
		ReflectionTestUtils.setField(timeLog, "lastStartedAt", Instant.now().minusSeconds(30));
		given(timeLogRepository.findFirstByUserIdAndRoomIdAndStatus(userId, roomId, TimeLogStatus.RUNNING))
				.willReturn(Optional.of(timeLog));

		timeLogPublicService.stopRunningRoomTimer(userId, roomId);

		assertThat(timeLog.getStatus()).isEqualTo(TimeLogStatus.ENDED);
		assertThat(timeLog.getEndedAt()).isNotNull();
		assertThat(timeLog.getDurationSeconds()).isGreaterThanOrEqualTo(30);
	}

	@Test
	void stopRunningRoomTimerIgnoresNullRoomId() {
		timeLogPublicService.stopRunningRoomTimer(UUID.randomUUID(), null);

		verifyNoInteractions(timeLogRepository);
	}

	@Test
	void stopRunningRoomTimerDoesNothingWhenNoRunningRoomTimerExists() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		given(timeLogRepository.findFirstByUserIdAndRoomIdAndStatus(userId, roomId, TimeLogStatus.RUNNING))
				.willReturn(Optional.empty());

		timeLogPublicService.stopRunningRoomTimer(userId, roomId);

		verify(timeLogRepository).findFirstByUserIdAndRoomIdAndStatus(userId, roomId, TimeLogStatus.RUNNING);
	}
}
