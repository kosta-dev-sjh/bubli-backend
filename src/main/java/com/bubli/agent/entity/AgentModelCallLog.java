package com.bubli.agent.entity;


import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "agent_model_call_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentModelCallLog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "job_id", nullable = false)
	private UUID jobId;

	@Column(name = "prompt_version", length = 40)
	private String promptVersion;

	@Column(name = "schema_version", length = 40)
	private String schemaVersion;

	@Column(name = "model_name", length = 100)
	private String modelName;

	@Column(name = "latency_ms")
	private Long latencyMs;

	@Column(name = "input_tokens")
	private Integer inputTokens;

	@Column(name = "output_tokens")
	private Integer outputTokens;

	@Column(name = "error_code", length = 80)
	private String errorCode;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public static AgentModelCallLog create(
			UUID jobId,
			String promptVersion,
			String schemaVersion,
			String modelName,
			Long latencyMs,
			Integer inputTokens,
			Integer outputTokens,
			String errorCode
	) {
		AgentModelCallLog log = new AgentModelCallLog();
		log.jobId = jobId;
		log.promptVersion = promptVersion;
		log.schemaVersion = schemaVersion;
		log.modelName = modelName;
		log.latencyMs = latencyMs;
		log.inputTokens = inputTokens;
		log.outputTokens = outputTokens;
		log.errorCode = errorCode;
		return log;
	}

	@PrePersist
	private void onCreate() {
		this.createdAt = Instant.now();
	}

}
