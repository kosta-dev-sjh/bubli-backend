package com.bubli.agent.entity;

import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "agent_suggestions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentSuggestion {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "room_id")
	private UUID roomId;

	@Column(name = "job_id")
	private UUID jobId;

	@Column(name = "resource_id")
	private UUID resourceId;

	@Enumerated(EnumType.STRING)
	@Column(name = "suggestion_type", nullable = false, length = 40)
	private AgentSuggestionType suggestionType;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
	private String payloadJson;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "evidence_json", columnDefinition = "jsonb")
	private String evidenceJson;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AgentSuggestionStatus status = AgentSuggestionStatus.DRAFT;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	private void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	private void onUpdate() {
		this.updatedAt = Instant.now();
	}

}
