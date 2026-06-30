package com.bubli.personal.notification.service;

import com.bubli.personal.notification.dto.NotificationResponse;
import com.bubli.personal.notification.type.NotificationSourceType;

import java.util.UUID;

public interface NotificationPublicService {

	NotificationResponse create(
			UUID userId,
			NotificationSourceType sourceType,
			UUID sourceId,
			String title,
			String body
	);
}
