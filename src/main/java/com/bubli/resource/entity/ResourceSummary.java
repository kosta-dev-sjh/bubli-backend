package com.bubli.resource.entity;

import com.bubli.resource.type.ResourceSummaryStatus;
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
@Table(name = "resource_summaries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceSummary {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "resource_id", nullable = false)
	private UUID resourceId;

	@Column(name = "job_id", nullable = false)
	private UUID jobId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "summary_json", nullable = false, columnDefinition = "jsonb")
	private String summaryJson;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "checklist_json", columnDefinition = "jsonb")
	private String checklistJson;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ResourceSummaryStatus status;

	@Column(name = "prompt_version", length = 40)
	private String promptVersion;

	@Column(name = "schema_version", length = 40)
	private String schemaVersion;

	@Column(name = "model_name", length = 100)
	private String modelName;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public static ResourceSummary create(UUID resourceId, UUID jobId, String summaryJson, String checklistJson,
			ResourceSummaryStatus status, String promptVersion, String schemaVersion, String modelName) {
		ResourceSummary summary = new ResourceSummary();
		summary.resourceId = resourceId;
		summary.jobId = jobId;
		summary.summaryJson = summaryJson;
		summary.checklistJson = checklistJson;
		summary.status = status;
		summary.promptVersion = promptVersion;
		summary.schemaVersion = schemaVersion;
		summary.modelName = modelName;
		return summary;
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
