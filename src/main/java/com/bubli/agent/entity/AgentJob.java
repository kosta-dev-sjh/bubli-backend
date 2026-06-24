package com.bubli.agent.entity;

import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "agent_jobs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentJob {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "requested_by_user_id", nullable = false)
	private UUID requestedByUserId;

	@Column(name = "room_id")
	private UUID roomId;

	@Column(name = "resource_id")
	private UUID resourceId;

	@Enumerated(EnumType.STRING)
	@Column(name = "job_type", nullable = false, length = 40)
	private AgentJobType jobType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AgentJobStatus status = AgentJobStatus.PENDING;

	@Column(name = "retry_count", nullable = false)
	private int retryCount;

	@Column(name = "error_code", length = 80)
	private String errorCode;

	@Column(name = "error_message", columnDefinition = "text")
	private String errorMessage;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

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
