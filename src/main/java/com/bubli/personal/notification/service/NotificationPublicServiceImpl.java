package com.bubli.personal.notification.service;

import com.bubli.personal.notification.dto.NotificationResponse;
import com.bubli.personal.notification.entity.Notification;
import com.bubli.personal.notification.repository.NotificationRepository;
import com.bubli.personal.notification.type.NotificationSourceType;
import com.bubli.personal.notification.type.NotificationStatus;
import com.bubli.websocket.service.WebSocketPublishPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationPublicServiceImpl implements NotificationPublicService {

	private final NotificationRepository notificationRepository;
	private final WebSocketPublishPublicService webSocketPublishPublicService;

	@Override
	@Transactional
	public NotificationResponse create(
			UUID userId,
			NotificationSourceType sourceType,
			UUID sourceId,
			String title,
			String body
	) {
		Notification notification = notificationRepository.save(Notification.create(
				userId,
				sourceType,
				sourceId,
				title,
				body
		));
		NotificationResponse response = NotificationResponse.from(notification);
		webSocketPublishPublicService.publishUserNotification(userId, response);
		return response;
	}

	@Override
	@Transactional
	public void createNotification(UUID userId, String sourceType, UUID sourceId, String title, String body) {
		create(
				userId,
				NotificationSourceType.valueOf(sourceType),
				sourceId,
				title,
				body
		);
	}

	@Override
	@Transactional(readOnly = true)
	public long countUnread(UUID userId) {
		return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD);
	}
}
