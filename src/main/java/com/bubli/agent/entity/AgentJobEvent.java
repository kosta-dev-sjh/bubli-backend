package com.bubli.agent.entity;


import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "agent_job_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentJobEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "job_id", nullable = false)
	private UUID jobId;

	@Column(name = "event_type", nullable = false, length = 60)
	private String eventType;

	@Column(columnDefinition = "text")
	private String message;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	private void onCreate() {
		this.createdAt = Instant.now();
	}

}
