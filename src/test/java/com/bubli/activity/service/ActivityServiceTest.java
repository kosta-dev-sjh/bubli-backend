package com.bubli.activity.service;

import com.bubli.activity.dto.RecordCurrentAppActivityCommand;
import com.bubli.activity.entity.ActivityLog;
import com.bubli.activity.repository.ActivityLogRepository;
import com.bubli.global.error.BusinessException;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.user.dto.UserResult;
import com.bubli.user.service.UserPublicService;
import com.bubli.user.type.ConsentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

	@Mock
	ActivityLogRepository activityLogRepository;

	@Mock
	UserPublicService userPublicService;

	@Mock
	ProjectMembershipPublicService projectMembershipPublicService;

	@InjectMocks
	ActivityService activityService;

	@Test
	void recordCurrentAppRejectsWhenActivityConsentIsDisabled() {
		UUID userId = UUID.randomUUID();
		given(userPublicService.isPrivacyConsentEnabled(userId, ConsentType.ACTIVITY_CONTEXT))
				.willReturn(false);

		assertThatThrownBy(() -> activityService.recordCurrentApp(userId, command(null)))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void recordCurrentAppStoresActivityWhenConsentIsEnabled() {
		UUID userId = UUID.randomUUID();
		UUID activityId = UUID.randomUUID();
		given(userPublicService.isPrivacyConsentEnabled(userId, ConsentType.ACTIVITY_CONTEXT))
				.willReturn(true);
		given(activityLogRepository.save(any(ActivityLog.class))).willAnswer(invocation -> {
			ActivityLog activityLog = invocation.getArgument(0);
			ReflectionTestUtils.setField(activityLog, "id", activityId);
			return activityLog;
		});

		var result = activityService.recordCurrentApp(userId, command(null));

		assertThat(result.id()).isEqualTo(activityId);
		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.appName()).isEqualTo("IntelliJ IDEA");
		assertThat(result.durationSeconds()).isEqualTo(1800L);
	}

	@Test
	void recordCurrentAppRequiresRoomMembershipWhenRoomIdExists() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		given(userPublicService.isPrivacyConsentEnabled(userId, ConsentType.ACTIVITY_CONTEXT))
				.willReturn(true);
		given(activityLogRepository.save(any(ActivityLog.class))).willAnswer(invocation -> invocation.getArgument(0));

		activityService.recordCurrentApp(userId, command(roomId));

		verify(projectMembershipPublicService).assertActiveMember(userId, roomId);
		ArgumentCaptor<ActivityLog> activityCaptor = ArgumentCaptor.forClass(ActivityLog.class);
		verify(activityLogRepository).save(activityCaptor.capture());
		assertThat(activityCaptor.getValue().getRoomId()).isEqualTo(roomId);
	}

	@Test
	void recordCurrentAppCalculatesDurationFromEndedAt() {
		UUID userId = UUID.randomUUID();
		given(userPublicService.isPrivacyConsentEnabled(userId, ConsentType.ACTIVITY_CONTEXT))
				.willReturn(true);
		given(activityLogRepository.save(any(ActivityLog.class))).willAnswer(invocation -> invocation.getArgument(0));

		var result = activityService.recordCurrentApp(userId, new RecordCurrentAppActivityCommand(
				null,
				"Chrome",
				"요구사항 문서",
				Instant.parse("2026-07-01T01:00:00Z"),
				Instant.parse("2026-07-01T01:15:00Z"),
				null
		));

		assertThat(result.durationSeconds()).isEqualTo(900L);
	}

	@Test
	void getTodayActivitiesUsesUserTimezone() {
		UUID userId = UUID.randomUUID();
		given(userPublicService.isPrivacyConsentEnabled(userId, ConsentType.ACTIVITY_CONTEXT))
				.willReturn(true);
		given(userPublicService.getUser(userId)).willReturn(new UserResult(
				userId,
				"bubli-id",
				"미연",
				null,
				"ko",
				"Asia/Tokyo"
		));
		given(activityLogRepository.findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
				any(),
				any(),
				any()
		)).willReturn(List.of());

		activityService.getTodayActivities(userId);

		verify(activityLogRepository)
				.findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
						org.mockito.ArgumentMatchers.eq(userId),
						any(),
						any()
				);
	}

	@Test
	void getTodayActivitiesRejectsWhenActivityConsentIsDisabled() {
		UUID userId = UUID.randomUUID();
		given(userPublicService.isPrivacyConsentEnabled(userId, ConsentType.ACTIVITY_CONTEXT))
				.willReturn(false);

		assertThatThrownBy(() -> activityService.getTodayActivities(userId))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void getActivityContextBetweenRejectsWhenActivityConsentIsDisabled() {
		UUID userId = UUID.randomUUID();
		given(userPublicService.isPrivacyConsentEnabled(userId, ConsentType.ACTIVITY_CONTEXT))
				.willReturn(false);

		assertThatThrownBy(() -> activityService.getActivityContextBetween(
				userId,
				Instant.parse("2026-07-01T00:00:00Z"),
				Instant.parse("2026-07-02T00:00:00Z"),
				20
		)).isInstanceOf(BusinessException.class);
	}

	@Test
	void deleteActivityRejectsOtherUserActivityAsNotFound() {
		UUID ownerId = UUID.randomUUID();
		UUID otherId = UUID.randomUUID();
		UUID activityId = UUID.randomUUID();
		ActivityLog activityLog = ActivityLog.create(
				ownerId,
				null,
				"Figma",
				"시안",
				Instant.now(),
				null,
				0L
		);
		ReflectionTestUtils.setField(activityLog, "id", activityId);
		given(activityLogRepository.findById(activityId)).willReturn(Optional.of(activityLog));

		assertThatThrownBy(() -> activityService.deleteActivity(otherId, activityId))
				.isInstanceOf(BusinessException.class);
	}

	private RecordCurrentAppActivityCommand command(UUID roomId) {
		return new RecordCurrentAppActivityCommand(
				roomId,
				"IntelliJ IDEA",
				"Bubli Backend",
				Instant.parse("2026-07-01T01:00:00Z"),
				Instant.parse("2026-07-01T01:30:00Z"),
				null
		);
	}
}
