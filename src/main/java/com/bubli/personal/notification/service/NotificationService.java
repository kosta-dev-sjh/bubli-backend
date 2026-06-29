package com.bubli.personal.notification.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.personal.notification.dto.NotificationResponse;
import com.bubli.personal.notification.entity.Notification;
import com.bubli.personal.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationRepository notificationRepository;

	@Transactional(readOnly = true)
	public Page<NotificationResponse> getNotifications(UUID userId, Pageable pageable) {
		return notificationRepository.findAllByUserId(userId, pageable)
				.map(NotificationResponse::from);
	}

	@Transactional
	public void readNotification(UUID userId, UUID notificationId) {
		Notification notification = getOwnedNotification(userId, notificationId);
		notification.markAsRead();
	}

	@Transactional
	public void archiveNotification(UUID userId, UUID notificationId) {
		Notification notification = getOwnedNotification(userId, notificationId);
		notification.markAsArchived();
	}

	private Notification getOwnedNotification(UUID userId, UUID notificationId) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_404_001));
		if (!notification.getUserId().equals(userId)) {
			throw new BusinessException(ErrorCode.NOTIFICATION_403_001);
		}
		return notification;
	}
}
