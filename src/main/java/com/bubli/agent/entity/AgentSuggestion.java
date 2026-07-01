package com.bubli.agent.entity;

import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.global.entity.BaseTimeEntity;
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

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "agent_suggestions",
        indexes = {
                @Index(name = "idx_agent_suggestions_user_status", columnList = "user_id,status"),
                @Index(name = "idx_agent_suggestions_room_status", columnList = "room_id,status"),
                @Index(name = "idx_agent_suggestions_job", columnList = "job_id"),
                @Index(name = "idx_agent_suggestions_resource", columnList = "resource_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentSuggestion extends BaseTimeEntity {

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
    private Map<String, Object> payloadJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_json", columnDefinition = "jsonb")
    private Map<String, Object> evidenceJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AgentSuggestionStatus status;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    private AgentSuggestion(
            UUID userId,
            UUID roomId,
            UUID jobId,
            UUID resourceId,
            AgentSuggestionType suggestionType,
            Map<String, Object> payloadJson,
            Map<String, Object> evidenceJson
    ) {
        this.userId = require(userId, "userId");
        this.roomId = roomId;
        this.jobId = jobId;
        this.resourceId = resourceId;
        this.suggestionType = require(suggestionType, "suggestionType");
        this.payloadJson = immutableJsonMap(require(payloadJson, "payloadJson"));
        this.evidenceJson = immutableJsonMap(evidenceJson);
        this.status = AgentSuggestionStatus.DRAFT;
    }

    public static AgentSuggestion draft(
            UUID userId,
            UUID roomId,
            UUID jobId,
            UUID resourceId,
            AgentSuggestionType suggestionType,
            Map<String, Object> payloadJson,
            Map<String, Object> evidenceJson
    ) {
        return new AgentSuggestion(
                userId,
                roomId,
                jobId,
                resourceId,
                suggestionType,
                payloadJson,
                evidenceJson
        );
    }

    public static AgentSuggestion createDraft(
            UUID userId,
            UUID roomId,
            UUID jobId,
            UUID resourceId,
            AgentSuggestionType suggestionType,
            String payloadJson,
            String evidenceJson
    ) {
        return draft(
                userId,
                roomId,
                jobId,
                resourceId,
                suggestionType,
                rawJson(payloadJson),
                rawJson(evidenceJson)
        );
    }

    public void update(AgentSuggestionStatus status, String payloadJson, String evidenceJson) {
        if (status != null) {
            this.status = status;
        }
        if (payloadJson != null) {
            this.payloadJson = immutableJsonMap(rawJson(payloadJson));
        }
        if (evidenceJson != null) {
            this.evidenceJson = immutableJsonMap(rawJson(evidenceJson));
        }
    }

    public void update(AgentSuggestionStatus status, Map<String, Object> payloadJson, Map<String, Object> evidenceJson) {
        if (status != null) {
            this.status = status;
        }
        if (payloadJson != null) {
            this.payloadJson = immutableJsonMap(payloadJson);
        }
        if (evidenceJson != null) {
            this.evidenceJson = immutableJsonMap(evidenceJson);
        }
    }

    public void approve(UUID reviewerId) {
        review(AgentSuggestionStatus.APPROVED, reviewerId);
    }

    public void hold(UUID reviewerId) {
        review(AgentSuggestionStatus.HELD, reviewerId);
    }

    public void reject(UUID reviewerId) {
        review(AgentSuggestionStatus.REJECTED, reviewerId);
    }

    public void modify(UUID reviewerId, Map<String, Object> modifiedPayloadJson) {
        ensureDraft();
        payloadJson = immutableJsonMap(require(modifiedPayloadJson, "modifiedPayloadJson"));
        reviewedBy = require(reviewerId, "reviewerId");
        reviewedAt = Instant.now();
    }

    public void edit(UUID reviewerId, Map<String, Object> editedContent) {
        modify(reviewerId, editedContent);
    }

    private void review(AgentSuggestionStatus nextStatus, UUID reviewerId) {
        ensureDraft();
        status = nextStatus;
        reviewedBy = require(reviewerId, "reviewerId");
        reviewedAt = Instant.now();
    }

    private void ensureDraft() {
        if (status != AgentSuggestionStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT suggestions can be changed.");
        }
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
