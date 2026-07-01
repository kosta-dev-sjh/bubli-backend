package com.bubli.personal.notification.entity;

import com.bubli.personal.notification.type.NotificationSourceType;
import com.bubli.personal.notification.type.NotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false, length = 30)
	private NotificationSourceType sourceType;

	@Column(name = "source_id")
	private UUID sourceId;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(columnDefinition = "text")
	private String body;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private NotificationStatus status = NotificationStatus.UNREAD;

	@Column(name = "read_at")
	private Instant readAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	private void onCreate() {
		this.createdAt = Instant.now();
	}

	public static Notification create(
			UUID userId,
			NotificationSourceType sourceType,
			UUID sourceId,
			String title,
			String body
	) {
		Notification notification = new Notification();
		notification.userId = userId;
		notification.sourceType = sourceType;
		notification.sourceId = sourceId;
		notification.title = title;
		notification.body = body;
		notification.status = NotificationStatus.UNREAD;
		return notification;
	}

	public void markAsRead() {
		this.status = NotificationStatus.READ;
		this.readAt = Instant.now();
	}

	public void markAsArchived() {
		this.status = NotificationStatus.ARCHIVED;
	}
}
