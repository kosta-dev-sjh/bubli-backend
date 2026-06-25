package com.bubli.resource.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.resource.type.AnalysisStatus;
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
                @Index(name = "idx_resource_summaries_resource", columnList = "resource_id")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AnalysisStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary_json", columnDefinition = "jsonb")
    private Map<String, Object> summaryJson;

    private ResourceSummary(UUID resourceId, AnalysisStatus status, Map<String, Object> summaryJson) {
        this.resourceId = require(resourceId, "resourceId");
        this.status = require(status, "status");
        this.summaryJson = immutableJsonMap(summaryJson);
    }

    public static ResourceSummary analyzed(UUID resourceId, Map<String, Object> summaryJson) {
        return new ResourceSummary(resourceId, AnalysisStatus.ANALYZED, summaryJson);
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
}
