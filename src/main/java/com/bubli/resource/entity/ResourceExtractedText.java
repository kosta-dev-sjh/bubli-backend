package com.bubli.resource.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.resource.type.ResourceExtractedTextStatus;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "resource_extracted_texts",
        indexes = {
                @Index(name = "idx_resource_extracted_texts_resource", columnList = "resource_id,updated_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceExtractedText extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "local_file_id", nullable = false, length = 120)
    private String localFileId;

    @Column(name = "checksum", length = 128)
    private String checksum;

    @Column(name = "extraction_method", nullable = false, length = 80)
    private String extractionMethod;

    @Column(name = "source_char_count", nullable = false)
    private int sourceCharCount;

    @Column(name = "analyzed_char_count", nullable = false)
    private int analyzedCharCount;

    @Column(name = "combined_text", nullable = false, columnDefinition = "TEXT")
    private String combinedText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sentences_json", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> sentencesJson;

    @Column(name = "text_truncated", nullable = false)
    private boolean textTruncated;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ResourceExtractedTextStatus status;

    private ResourceExtractedText(
            UUID resourceId,
            String localFileId,
            String checksum,
            String extractionMethod,
            int sourceCharCount,
            int analyzedCharCount,
            String combinedText,
            List<Map<String, Object>> sentencesJson,
            boolean textTruncated
    ) {
        this.resourceId = require(resourceId, "resourceId");
        this.localFileId = requireText(localFileId, "localFileId");
        this.checksum = blankToNull(checksum);
        this.extractionMethod = requireText(extractionMethod, "extractionMethod");
        this.sourceCharCount = sourceCharCount;
        this.analyzedCharCount = analyzedCharCount;
        this.combinedText = requireText(combinedText, "combinedText");
        this.sentencesJson = immutableJsonList(sentencesJson);
        this.textTruncated = textTruncated;
        this.status = ResourceExtractedTextStatus.STORED;
    }

    public static ResourceExtractedText create(
            UUID resourceId,
            String localFileId,
            String checksum,
            String extractionMethod,
            int sourceCharCount,
            int analyzedCharCount,
            String combinedText,
            List<Map<String, Object>> sentencesJson,
            boolean textTruncated
    ) {
        return new ResourceExtractedText(
                resourceId,
                localFileId,
                checksum,
                extractionMethod,
                sourceCharCount,
                analyzedCharCount,
                combinedText,
                sentencesJson,
                textTruncated
        );
    }

    public void replaceWith(
            String checksum,
            int sourceCharCount,
            int analyzedCharCount,
            String combinedText,
            List<Map<String, Object>> sentencesJson,
            boolean textTruncated
    ) {
        this.checksum = blankToNull(checksum);
        this.sourceCharCount = sourceCharCount;
        this.analyzedCharCount = analyzedCharCount;
        this.combinedText = requireText(combinedText, "combinedText");
        this.sentencesJson = immutableJsonList(sentencesJson);
        this.textTruncated = textTruncated;
        this.status = ResourceExtractedTextStatus.STORED;
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static List<Map<String, Object>> immutableJsonList(List<Map<String, Object>> value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("sentencesJson is required.");
        }
        List<Map<String, Object>> copy = new ArrayList<>();
        for (Map<String, Object> item : value) {
            copy.add(Collections.unmodifiableMap(new LinkedHashMap<>(item)));
        }
        return Collections.unmodifiableList(copy);
    }
}
