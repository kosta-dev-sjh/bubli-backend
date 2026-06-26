package com.bubli.personal.timer.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.personal.timer.dto.StartTimeLogCommand;
import com.bubli.personal.timer.dto.TimeLogResult;
import com.bubli.personal.timer.entity.TimeLog;
import com.bubli.personal.timer.repository.TimeLogRepository;
import com.bubli.personal.timer.type.TimeLogStatus;
import com.bubli.personal.timer.type.TimerType;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.RoomMemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimeLogService {

	private final TimeLogRepository timeLogRepository;
	private final RoomMemberRepository roomMemberRepository;

	@Transactional
	public TimeLogResult start(StartTimeLogCommand command) {
		return timeLogRepository.findByIdempotencyKey(command.idempotencyKey())
				.map(existing -> TimeLogResult.from(validateIdempotentOwner(existing, command.userId())))
				.orElseGet(() -> TimeLogResult.from(createTimeLog(command)));
	}

	@Transactional
	public TimeLogResult pause(UUID userId, UUID timeLogId) {
		TimeLog timeLog = getAccessibleTimeLog(userId, timeLogId);
		if (!TimeLogStatus.RUNNING.equals(timeLog.getStatus())) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}
		timeLog.pause(Instant.now());
		return TimeLogResult.from(timeLog);
	}

	@Transactional
	public TimeLogResult resume(UUID userId, UUID timeLogId) {
		TimeLog timeLog = getAccessibleTimeLog(userId, timeLogId);
		if (!TimeLogStatus.PAUSED.equals(timeLog.getStatus())
				&& !TimeLogStatus.NEEDS_RECOVERY.equals(timeLog.getStatus())) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}
		timeLog.resume(Instant.now());
		return TimeLogResult.from(timeLog);
	}

	@Transactional
	public TimeLogResult stop(UUID userId, UUID timeLogId) {
		TimeLog timeLog = getAccessibleTimeLog(userId, timeLogId);
		if (TimeLogStatus.ENDED.equals(timeLog.getStatus())) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}
		timeLog.stop(Instant.now());
		return TimeLogResult.from(timeLog);
	}

	@Transactional
	public TimeLogResult heartbeat(UUID userId, UUID timeLogId) {
		TimeLog timeLog = getAccessibleTimeLog(userId, timeLogId);
		if (!TimeLogStatus.RUNNING.equals(timeLog.getStatus())) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}
		timeLog.heartbeat(Instant.now());
		return TimeLogResult.from(timeLog);
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
		TimeLog timeLog = timeLogRepository.findById(timeLogId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PERSONAL_404_001));
		if (!userId.equals(timeLog.getUserId())) {
			throw new BusinessException(ErrorCode.WORK_403_001);
		}
		if (timeLog.getRoomId() != null) {
			checkRoomMember(userId, timeLog.getRoomId());
		}
		return timeLog;
	}

	private void checkRoomMember(UUID userId, UUID roomId) {
		boolean activeMember = roomMemberRepository.existsByRoomIdAndUserIdAndStatus(
				roomId,
				userId,
				RoomMemberStatus.ACTIVE
		);
		if (!activeMember) {
			throw new BusinessException(ErrorCode.PROJECT_403_001);
		}
	}
}
