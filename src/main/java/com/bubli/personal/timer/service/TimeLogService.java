package com.bubli.personal.timer.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.personal.timer.dto.StartTimeLogCommand;
import com.bubli.personal.timer.dto.TimeLogResponse;
import com.bubli.personal.timer.dto.TimeLogResult;
import com.bubli.personal.timer.entity.TimeLog;
import com.bubli.personal.timer.repository.TimeLogRepository;
import com.bubli.personal.timer.type.TimeLogStatus;
import com.bubli.personal.timer.type.TimerType;
import com.bubli.project.service.ProjectMembershipPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimeLogService {

	private final TimeLogRepository timeLogRepository;
	private final ProjectMembershipPublicService projectMembershipPublicService;

	@Transactional
	public TimeLogResponse start(StartTimeLogCommand command) {
		TimeLogResult result = timeLogRepository.findByIdempotencyKey(command.idempotencyKey())
				.map(existing -> TimeLogResult.from(validateIdempotentOwner(existing, command.userId())))
				.orElseGet(() -> {
					if (timeLogRepository.existsByUserIdAndStatus(command.userId(), TimeLogStatus.RUNNING)) {
						throw new BusinessException(ErrorCode.PERSONAL_409_001);
					}
					return TimeLogResult.from(createTimeLog(command));
				});
		return TimeLogResponse.from(result);
	}

	@Transactional
	public TimeLogResponse pause(UUID userId, UUID timeLogId) {
		TimeLog timeLog = getAccessibleTimeLog(userId, timeLogId);
		if (!TimeLogStatus.RUNNING.equals(timeLog.getStatus())) {
			throw new BusinessException(ErrorCode.PERSONAL_400_001);
		}
		timeLog.pause(Instant.now());
		return TimeLogResponse.from(TimeLogResult.from(timeLog));
	}

	@Transactional
	public TimeLogResponse resume(UUID userId, UUID timeLogId) {
		TimeLog timeLog = getAccessibleTimeLog(userId, timeLogId);
		if (!TimeLogStatus.PAUSED.equals(timeLog.getStatus())
				&& !TimeLogStatus.NEEDS_RECOVERY.equals(timeLog.getStatus())) {
			throw new BusinessException(ErrorCode.PERSONAL_400_001);
		}
		timeLog.resume(Instant.now());
		return TimeLogResponse.from(TimeLogResult.from(timeLog));
	}

	@Transactional
	public TimeLogResponse stop(UUID userId, UUID timeLogId) {
		TimeLog timeLog = getAccessibleTimeLog(userId, timeLogId);
		if (!TimeLogStatus.RUNNING.equals(timeLog.getStatus())
				&& !TimeLogStatus.PAUSED.equals(timeLog.getStatus())) {
			throw new BusinessException(ErrorCode.PERSONAL_400_001);
		}
		timeLog.stop(Instant.now());
		return TimeLogResponse.from(TimeLogResult.from(timeLog));
	}

	@Transactional
	public TimeLogResponse heartbeat(UUID userId, UUID timeLogId) {
		TimeLog timeLog = getAccessibleTimeLog(userId, timeLogId);
		if (!TimeLogStatus.RUNNING.equals(timeLog.getStatus())) {
			throw new BusinessException(ErrorCode.PERSONAL_400_001);
		}
		timeLog.heartbeat(Instant.now());
		return TimeLogResponse.from(TimeLogResult.from(timeLog));
	}

	private TimeLog createTimeLog(StartTimeLogCommand command) {
		TimerType timerType = command.timerType() == null ? TimerType.GENERAL : command.timerType();
		if (TimerType.GENERAL.equals(timerType) && command.roomId() != null) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}
		if (command.roomId() != null) {
			checkRoomMember(command.userId(), command.roomId());
		}
		TimeLog timeLog = TimeLog.start(
				command.userId(),
				command.roomId(),
				timerType,
				command.idempotencyKey(),
				command.recoveredFromTimeLogId(),
				Instant.now()
		);
		return timeLogRepository.save(timeLog);
	}

	private TimeLog validateIdempotentOwner(TimeLog timeLog, UUID userId) {
		if (!userId.equals(timeLog.getUserId())) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}
		return timeLog;
	}

	private TimeLog getAccessibleTimeLog(UUID userId, UUID timeLogId) {
		TimeLog timeLog = timeLogRepository.findByIdAndUserId(timeLogId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PERSONAL_404_001));
		if (timeLog.getRoomId() != null) {
			checkRoomMember(userId, timeLog.getRoomId());
		}
		return timeLog;
	}

	private void checkRoomMember(UUID userId, UUID roomId) {
		projectMembershipPublicService.assertActiveMember(userId, roomId);
	}
}
