package com.bubli.resource.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.resource.type.AnalysisStatus;
import com.bubli.resource.type.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "ai_documents",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ai_documents_resource", columnNames = "resource_id")
        },
        indexes = {
                @Index(name = "idx_ai_documents_room_status", columnList = "room_id,status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiDocument extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "room_id")
    private UUID roomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType;

    @Column(name = "detected_confidence", precision = 5, scale = 4)
    private BigDecimal detectedConfidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AnalysisStatus status;

    private AiDocument(
            UUID resourceId,
            UUID roomId,
            DocumentType documentType,
            BigDecimal detectedConfidence,
            AnalysisStatus status
    ) {
        this.resourceId = require(resourceId, "resourceId");
        this.roomId = roomId;
        this.documentType = require(documentType, "documentType");
        this.detectedConfidence = validateConfidence(detectedConfidence);
        this.status = require(status, "status");
    }

    public static AiDocument analyzed(
            UUID resourceId,
            UUID roomId,
            DocumentType documentType,
            BigDecimal detectedConfidence
    ) {
        return new AiDocument(resourceId, roomId, documentType, detectedConfidence, AnalysisStatus.ANALYZED);
    }

    private static BigDecimal validateConfidence(BigDecimal confidence) {
        if (confidence == null) {
            return null;
        }
        if (confidence.compareTo(BigDecimal.ZERO) < 0 || confidence.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("detectedConfidence must be between 0 and 1.");
        }
        return confidence;
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value;
    }
}
