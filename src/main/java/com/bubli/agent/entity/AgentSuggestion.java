package com.bubli.agent.entity;

import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.global.entity.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "agent_suggestions",
        indexes = {
                @Index(name = "idx_agent_suggestions_room_status", columnList = "project_room_id,status"),
                @Index(name = "idx_agent_suggestions_request", columnList = "agent_request_id"),
                @Index(name = "idx_agent_suggestions_source_document", columnList = "source_document_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentSuggestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "agent_request_id", nullable = false)
    private UUID agentRequestId;

    @Column(name = "project_room_id", nullable = false)
    private UUID projectRoomId;

    @Column(name = "source_document_id")
    private UUID sourceDocumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggestion_type", nullable = false, length = 40)
    private AgentSuggestionType suggestionType;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "original_content_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> originalContentJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> contentJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AgentSuggestionStatus status;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @OneToMany(
            mappedBy = "suggestion",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private final List<SuggestionEvidence> evidences = new ArrayList<>();

    private AgentSuggestion(
            UUID agentRequestId,
            UUID projectRoomId,
            UUID sourceDocumentId,
            AgentSuggestionType suggestionType,
            String title,
            Map<String, Object> contentJson,
            BigDecimal confidence
    ) {
        this.agentRequestId = require(agentRequestId, "agentRequestId");
        this.projectRoomId = require(projectRoomId, "projectRoomId");
        this.sourceDocumentId = sourceDocumentId;
        this.suggestionType = require(suggestionType, "suggestionType");
        this.title = requireText(title, "title");
        this.originalContentJson = immutableJsonMap(require(contentJson, "contentJson"));
        this.contentJson = immutableJsonMap(contentJson);
        this.confidence = validateConfidence(confidence);
        this.status = AgentSuggestionStatus.DRAFT;
    }

    public static AgentSuggestion pending(
            UUID agentRequestId,
            UUID projectRoomId,
            UUID sourceDocumentId,
            AgentSuggestionType suggestionType,
            String title,
            Map<String, Object> contentJson,
            BigDecimal confidence
    ) {
        return new AgentSuggestion(
                agentRequestId,
                projectRoomId,
                sourceDocumentId,
                suggestionType,
                title,
                contentJson,
                confidence
        );
    }

    public SuggestionEvidence addEvidence(
            UUID documentId,
            UUID chunkId,
            String fileName,
            Integer pageNumber,
            String evidenceText,
            BigDecimal similarityScore
    ) {
        ensureDraft();
        SuggestionEvidence evidence = SuggestionEvidence.create(
                this,
                documentId,
                chunkId,
                fileName,
                pageNumber,
                evidenceText,
                similarityScore
        );
        evidences.add(evidence);
        return evidence;
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

    public void modify(UUID reviewerId, Map<String, Object> modifiedContentJson) {
        ensureDraft();
        contentJson = immutableJsonMap(require(modifiedContentJson, "modifiedContentJson"));
        reviewedBy = require(reviewerId, "reviewerId");
        reviewedAt = Instant.now();
    }

    public List<SuggestionEvidence> getEvidences() {
        return Collections.unmodifiableList(evidences);
    }

    private void review(AgentSuggestionStatus nextStatus, UUID reviewerId) {
        ensureDraft();
        status = nextStatus;
        reviewedBy = require(reviewerId, "reviewerId");
        reviewedAt = Instant.now();
    }

    private void ensureDraft() {
        if (status != AgentSuggestionStatus.DRAFT) {
            throw new IllegalStateException("검토가 끝난 제안은 다시 변경할 수 없습니다.");
        }
    }

    private static BigDecimal validateConfidence(BigDecimal confidence) {
        if (confidence == null) {
            return null;
        }
        if (confidence.compareTo(BigDecimal.ZERO) < 0 || confidence.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("confidence는 0과 1 사이여야 합니다.");
        }
        return confidence;
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + "는 필수입니다.");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "는 필수입니다.");
        }
        return value;
    }

    private static Map<String, Object> immutableJsonMap(Map<String, Object> value) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }
}
