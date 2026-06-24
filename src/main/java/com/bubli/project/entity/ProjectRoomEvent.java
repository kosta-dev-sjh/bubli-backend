package com.bubli.project.entity;

import com.bubli.global.entity.CreatedAtEntity;
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
public class ProjectRoomEvent extends CreatedAtEntity {

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

}
