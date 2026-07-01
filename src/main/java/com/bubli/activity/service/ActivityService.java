package com.bubli.activity.service;

import com.bubli.activity.dto.ActivityLogResult;
import com.bubli.activity.dto.RecordCurrentAppActivityCommand;
import com.bubli.activity.entity.ActivityLog;
import com.bubli.activity.repository.ActivityLogRepository;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.user.dto.UserResult;
import com.bubli.user.service.UserPublicService;
import com.bubli.user.type.ConsentType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityService implements ActivityPublicService {

	private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");

	private final ActivityLogRepository activityLogRepository;
	private final UserPublicService userPublicService;
	private final ProjectMembershipPublicService projectMembershipPublicService;

	@Transactional
	public ActivityLogResult recordCurrentApp(UUID userId, RecordCurrentAppActivityCommand command) {
		assertActivityConsent(userId);
		if (command.roomId() != null) {
			projectMembershipPublicService.assertActiveMember(userId, command.roomId());
		}
		long durationSeconds = durationSeconds(command);
		ActivityLog activityLog = ActivityLog.create(
				userId,
				command.roomId(),
				command.appName(),
				command.windowTitle(),
				command.startedAt(),
				command.endedAt(),
				durationSeconds
		);
		return ActivityLogResult.from(activityLogRepository.save(activityLog));
	}

	@Transactional(readOnly = true)
	public List<ActivityLogResult> getTodayActivities(UUID userId) {
		assertActivityConsent(userId);
		ZoneId zoneId = userZone(userId);
		LocalDate today = LocalDate.now(zoneId);
		Instant from = today.atStartOfDay(zoneId).toInstant();
		Instant to = today.plusDays(1).atStartOfDay(zoneId).toInstant();
		return activityLogRepository
				.findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(userId, from, to)
				.stream()
				.map(ActivityLogResult::from)
				.toList();
	}

	@Transactional
	public void deleteActivity(UUID userId, UUID activityId) {
		ActivityLog activityLog = activityLogRepository.findById(activityId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ACTIVITY_404_001));
		if (!activityLog.getUserId().equals(userId)) {
			throw new BusinessException(ErrorCode.ACTIVITY_404_001);
		}
		activityLogRepository.delete(activityLog);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ActivityLogResult> getActivityContextBetween(UUID userId, Instant from, Instant to, int limit) {
		assertActivityConsent(userId);
		return activityLogRepository
				.findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByDurationSecondsDesc(
						userId,
						from,
						to,
						Pageable.ofSize(limit)
				)
				.stream()
				.map(ActivityLogResult::from)
				.toList();
	}

	private void assertActivityConsent(UUID userId) {
		if (!userPublicService.isPrivacyConsentEnabled(userId, ConsentType.ACTIVITY_CONTEXT)) {
			throw new BusinessException(ErrorCode.ACTIVITY_403_001);
		}
	}

	private long durationSeconds(RecordCurrentAppActivityCommand command) {
		if (command.endedAt() != null && command.endedAt().isBefore(command.startedAt())) {
			throw new BusinessException(ErrorCode.ACTIVITY_400_001);
		}
		if (command.durationSeconds() != null) {
			return command.durationSeconds();
		}
		if (command.endedAt() == null) {
			return 0L;
		}
		return Duration.between(command.startedAt(), command.endedAt()).toSeconds();
	}

	private ZoneId userZone(UUID userId) {
		UserResult user = userPublicService.getUser(userId);
		if (user.timezone() == null || user.timezone().isBlank()) {
			return DEFAULT_ZONE;
		}
		try {
			return ZoneId.of(user.timezone());
		} catch (DateTimeException exception) {
			return DEFAULT_ZONE;
		}
	}
}
