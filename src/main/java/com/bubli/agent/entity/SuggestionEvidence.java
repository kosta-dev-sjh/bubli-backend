package com.bubli.agent.entity;

import com.bubli.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "suggestion_evidences",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_suggestion_evidences_suggestion_chunk",
                        columnNames = {"suggestion_id", "chunk_id"}
                )
        },
        indexes = {
                @Index(name = "idx_suggestion_evidences_document", columnList = "document_id"),
                @Index(name = "idx_suggestion_evidences_chunk", columnList = "chunk_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SuggestionEvidence extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "suggestion_id", nullable = false)
    private AgentSuggestion suggestion;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "chunk_id", nullable = false)
    private UUID chunkId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "evidence_text", nullable = false, columnDefinition = "text")
    private String evidenceText;

    @Column(name = "similarity_score", nullable = false, precision = 8, scale = 7)
    private BigDecimal similarityScore;

    private SuggestionEvidence(
            AgentSuggestion suggestion,
            UUID documentId,
            UUID chunkId,
            String fileName,
            Integer pageNumber,
            String evidenceText,
            BigDecimal similarityScore
    ) {
        this.suggestion = require(suggestion, "suggestion");
        this.documentId = require(documentId, "documentId");
        this.chunkId = require(chunkId, "chunkId");
        this.fileName = requireText(fileName, "fileName");
        if (pageNumber != null && pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber는 1 이상이어야 합니다.");
        }
        this.pageNumber = pageNumber;
        this.evidenceText = requireText(evidenceText, "evidenceText");
        this.similarityScore = validateScore(similarityScore);
    }

    static SuggestionEvidence create(
            AgentSuggestion suggestion,
            UUID documentId,
            UUID chunkId,
            String fileName,
            Integer pageNumber,
            String evidenceText,
            BigDecimal similarityScore
    ) {
        return new SuggestionEvidence(
                suggestion,
                documentId,
                chunkId,
                fileName,
                pageNumber,
                evidenceText,
                similarityScore
        );
    }

    private static BigDecimal validateScore(BigDecimal score) {
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("similarityScore는 0과 1 사이여야 합니다.");
        }
        return score;
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
}
