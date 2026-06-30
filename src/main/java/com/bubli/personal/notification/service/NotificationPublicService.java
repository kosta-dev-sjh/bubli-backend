package com.bubli.personal.notification.service;

import java.util.UUID;

public interface NotificationPublicService {

	void createNotification(UUID userId, String sourceType, UUID sourceId, String title, String body);

	long countUnread(UUID userId);
}
