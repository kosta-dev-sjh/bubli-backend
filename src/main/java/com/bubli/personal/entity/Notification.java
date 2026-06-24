package com.bubli.personal.entity;

import com.bubli.global.entity.CreatedAtEntity;
import com.bubli.personal.type.NotificationSourceType;
import com.bubli.personal.type.NotificationStatus;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends CreatedAtEntity {

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

}
