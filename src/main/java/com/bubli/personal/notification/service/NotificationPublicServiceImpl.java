package com.bubli.personal.notification.service;

import com.bubli.personal.notification.entity.Notification;
import com.bubli.personal.notification.repository.NotificationRepository;
import com.bubli.personal.notification.type.NotificationSourceType;
import com.bubli.personal.notification.type.NotificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationPublicServiceImpl implements NotificationPublicService {

	private final NotificationRepository notificationRepository;

	@Override
	@Transactional
	public void createNotification(UUID userId, String sourceType, UUID sourceId, String title, String body) {
		Notification notification = Notification.create(
				userId,
				NotificationSourceType.valueOf(sourceType),
				sourceId,
				title,
				body
		);
		notificationRepository.save(notification);
	}

	@Override
	@Transactional(readOnly = true)
	public long countUnread(UUID userId) {
		return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD);
	}
}
