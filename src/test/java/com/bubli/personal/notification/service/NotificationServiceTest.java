package com.bubli.personal.notification.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.personal.notification.dto.NotificationResponse;
import com.bubli.personal.notification.entity.Notification;
import com.bubli.personal.notification.repository.NotificationRepository;
import com.bubli.personal.notification.type.NotificationSourceType;
import com.bubli.personal.notification.type.NotificationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	@Mock
	NotificationRepository notificationRepository;

	@InjectMocks
	NotificationService notificationService;

	@Test
	void getNotificationsReturnsPageForUser() {
		UUID userId = UUID.randomUUID();
		Notification notification = Notification.create(
				userId,
				NotificationSourceType.COMMENT,
				UUID.randomUUID(),
				"새 댓글이 달렸습니다",
				"자료에 댓글이 달렸습니다"
		);
		Pageable pageable = PageRequest.of(0, 20);
		Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);
		given(notificationRepository.findAllByUserIdAndStatusNot(userId, NotificationStatus.ARCHIVED, pageable))
				.willReturn(page);

		Page<NotificationResponse> result = notificationService.getNotifications(userId, pageable);

		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().getFirst().title()).isEqualTo("새 댓글이 달렸습니다");
		assertThat(result.getContent().getFirst().status()).isEqualTo(NotificationStatus.UNREAD);
	}

	@Test
	void getNotificationsCanFilterByStatus() {
		UUID userId = UUID.randomUUID();
		Notification notification = Notification.create(
				userId,
				NotificationSourceType.AGENT,
				UUID.randomUUID(),
				"AI ?뚮┝",
				null
		);
		Pageable pageable = PageRequest.of(0, 20);
		Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);
		given(notificationRepository.findAllByUserIdAndStatus(userId, NotificationStatus.UNREAD, pageable))
				.willReturn(page);

		Page<NotificationResponse> result = notificationService.getNotifications(userId, NotificationStatus.UNREAD, pageable);

		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().getFirst().status()).isEqualTo(NotificationStatus.UNREAD);
	}

	@Test
	void readNotificationMarksAsReadForOwner() {
		UUID userId = UUID.randomUUID();
		UUID notificationId = UUID.randomUUID();
		Notification notification = Notification.create(
				userId,
				NotificationSourceType.MESSAGE,
				UUID.randomUUID(),
				"새 메시지가 도착했습니다",
				null
		);
		ReflectionTestUtils.setField(notification, "id", notificationId);
		given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

		notificationService.readNotification(userId, notificationId);

		assertThat(notification.getStatus()).isEqualTo(NotificationStatus.READ);
		assertThat(notification.getReadAt()).isNotNull();
	}

	@Test
	void readNotificationRejectsOtherUsersNotification() {
		UUID ownerId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		UUID notificationId = UUID.randomUUID();
		Notification notification = Notification.create(
				ownerId,
				NotificationSourceType.RESOURCE,
				UUID.randomUUID(),
				"자료가 분석되었습니다",
				null
		);
		ReflectionTestUtils.setField(notification, "id", notificationId);
		given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

		assertThatThrownBy(() -> notificationService.readNotification(otherUserId, notificationId))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_403_001));
		assertThat(notification.getStatus()).isEqualTo(NotificationStatus.UNREAD);
	}

	@Test
	void archiveNotificationMarksAsArchivedForOwner() {
		UUID userId = UUID.randomUUID();
		UUID notificationId = UUID.randomUUID();
		Notification notification = Notification.create(
				userId,
				NotificationSourceType.AGENT,
				UUID.randomUUID(),
				"AI 제안이 생성되었습니다",
				null
		);
		ReflectionTestUtils.setField(notification, "id", notificationId);
		given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

		notificationService.archiveNotification(userId, notificationId);

		assertThat(notification.getStatus()).isEqualTo(NotificationStatus.ARCHIVED);
	}
}
