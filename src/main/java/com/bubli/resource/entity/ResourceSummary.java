package com.bubli.resource.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.resource.type.AnalysisStatus;
import com.bubli.resource.type.ResourceSummaryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "resource_summaries",
        indexes = {
                @Index(name = "idx_resource_summaries_resource", columnList = "resource_id"),
                @Index(name = "idx_resource_summaries_job", columnList = "job_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceSummary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "job_id")
    private UUID jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AnalysisStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary_json", columnDefinition = "jsonb")
    private Map<String, Object> summaryJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checklist_json", columnDefinition = "jsonb")
    private Map<String, Object> checklistJson;

    @Column(name = "prompt_version", length = 50)
    private String promptVersion;

    @Column(name = "schema_version", length = 50)
    private String schemaVersion;

    @Column(name = "model_name", length = 100)
    private String modelName;

    private ResourceSummary(
            UUID resourceId,
            UUID jobId,
            AnalysisStatus status,
            Map<String, Object> summaryJson,
            Map<String, Object> checklistJson,
            String promptVersion,
            String schemaVersion,
            String modelName
    ) {
        this.resourceId = require(resourceId, "resourceId");
        this.jobId = jobId;
        this.status = require(status, "status");
        this.summaryJson = immutableJsonMap(summaryJson);
        this.checklistJson = immutableJsonMap(checklistJson);
        this.promptVersion = promptVersion;
        this.schemaVersion = schemaVersion;
        this.modelName = modelName;
    }

    public static ResourceSummary analyzed(UUID resourceId, UUID jobId, Map<String, Object> summaryJson) {
        return new ResourceSummary(
                resourceId,
                jobId,
                AnalysisStatus.ANALYZED,
                summaryJson,
                null,
                null,
                null,
                null
        );
    }

    public static ResourceSummary create(
            UUID resourceId,
            UUID jobId,
            String summaryJson,
            String checklistJson,
            ResourceSummaryStatus status,
            String promptVersion,
            String schemaVersion,
            String modelName
    ) {
        return new ResourceSummary(
                resourceId,
                jobId,
                AnalysisStatus.valueOf(require(status, "status").name()),
                rawJson(summaryJson),
                rawJson(checklistJson),
                promptVersion,
                schemaVersion,
                modelName
        );
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value;
    }

    private static Map<String, Object> immutableJsonMap(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }

    private static Map<String, Object> rawJson(String value) {
        if (value == null) {
            return null;
        }
        return Map.of("raw", value);
    }
}
