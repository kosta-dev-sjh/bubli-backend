package com.bubli.resource.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.resource.type.ResourceSummaryStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "resource_summaries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceSummary extends BaseTimeEntity {

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

}
