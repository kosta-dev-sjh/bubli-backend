package com.bubli.personal.notification.dto;

import com.bubli.personal.notification.entity.Notification;
import com.bubli.personal.notification.type.NotificationSourceType;
import com.bubli.personal.notification.type.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
		UUID id,
		NotificationSourceType sourceType,
		UUID sourceId,
		String title,
		String body,
		NotificationStatus status,
		Instant readAt,
		Instant createdAt
) {
	public static NotificationResponse from(Notification notification) {
		return new NotificationResponse(
				notification.getId(),
				notification.getSourceType(),
				notification.getSourceId(),
				notification.getTitle(),
				notification.getBody(),
				notification.getStatus(),
				notification.getReadAt(),
				notification.getCreatedAt()
		);
	}
}
