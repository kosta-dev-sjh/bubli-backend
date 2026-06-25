package com.bubli.agent.entity;

import com.bubli.agent.type.AgentDispatchOutboxStatus;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "agent_dispatch_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentDispatchOutbox {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "job_id", nullable = false, unique = true)
	private UUID jobId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
	private String payloadJson;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AgentDispatchOutboxStatus status = AgentDispatchOutboxStatus.PENDING;

	@Column(name = "retry_count", nullable = false)
	private int retryCount;

	@Column(name = "error_code", length = 80)
	private String errorCode;

	@Column(name = "error_message", columnDefinition = "text")
	private String errorMessage;

	@Column(name = "dispatched_at")
	private Instant dispatchedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	private AgentDispatchOutbox(UUID jobId, String payloadJson) {
		this.jobId = jobId;
		this.payloadJson = payloadJson;
		this.status = AgentDispatchOutboxStatus.PENDING;
		this.retryCount = 0;
	}

	public static AgentDispatchOutbox pending(UUID jobId, String payloadJson) {
		return new AgentDispatchOutbox(jobId, payloadJson);
	}

	public void markDispatched() {
		this.status = AgentDispatchOutboxStatus.DISPATCHED;
		this.errorCode = null;
		this.errorMessage = null;
		this.dispatchedAt = Instant.now();
	}

	public void markFailed(String errorCode, String errorMessage) {
		this.status = AgentDispatchOutboxStatus.FAILED;
		this.retryCount++;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.dispatchedAt = null;
	}

	public void markDeadLetter(String errorCode, String errorMessage) {
		this.status = AgentDispatchOutboxStatus.DEAD_LETTER;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.dispatchedAt = null;
	}

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
