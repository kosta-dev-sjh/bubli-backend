package com.bubli.project.entity;

import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "project_room_events",
	uniqueConstraints = @UniqueConstraint(name = "uk_project_room_events_room_sequence", columnNames = {"room_id", "sequence"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectRoomEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "room_id", nullable = false)
	private UUID roomId;

	@Column(nullable = false)
	private Long sequence;

	@Column(name = "event_type", nullable = false, length = 60)
	private String eventType;

	@Column(name = "actor_user_id")
	private UUID actorUserId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
	private String payloadJson;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public static ProjectRoomEvent create(
			UUID roomId,
			Long sequence,
			String eventType,
			UUID actorUserId,
			String payloadJson,
			Instant occurredAt
	) {
		ProjectRoomEvent event = new ProjectRoomEvent();
		event.roomId = roomId;
		event.sequence = sequence;
		event.eventType = eventType;
		event.actorUserId = actorUserId;
		event.payloadJson = payloadJson;
		event.occurredAt = occurredAt == null ? Instant.now() : occurredAt;
		return event;
	}

	@PrePersist
	private void onCreate() {
		this.createdAt = Instant.now();
	}

}
