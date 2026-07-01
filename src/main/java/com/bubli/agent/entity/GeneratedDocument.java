package com.bubli.agent.entity;

import com.bubli.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "generated_documents",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_generated_documents_suggestion", columnNames = "suggestion_id")
        },
        indexes = {
                @Index(name = "idx_generated_documents_user", columnList = "user_id"),
                @Index(name = "idx_generated_documents_room", columnList = "room_id"),
                @Index(name = "idx_generated_documents_resource", columnList = "resource_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GeneratedDocument extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "suggestion_id", nullable = false)
    private UUID suggestionId;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "document_type", nullable = false, length = 60)
    private String documentType;

    @Column(name = "content_markdown", nullable = false, columnDefinition = "TEXT")
    private String contentMarkdown;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private Map<String, Object> metadataJson;

    private GeneratedDocument(
            UUID userId,
            UUID roomId,
            UUID suggestionId,
            UUID resourceId,
            String title,
            String documentType,
            String contentMarkdown,
            Map<String, Object> metadataJson
    ) {
        this.userId = require(userId, "userId");
        this.roomId = roomId;
        this.suggestionId = require(suggestionId, "suggestionId");
        this.resourceId = resourceId;
        this.title = requireText(title, "title");
        this.documentType = requireText(documentType, "documentType");
        this.contentMarkdown = requireText(contentMarkdown, "contentMarkdown");
        this.metadataJson = immutableJsonMap(metadataJson);
    }

    public static GeneratedDocument create(
            UUID userId,
            UUID roomId,
            UUID suggestionId,
            UUID resourceId,
            String title,
            String documentType,
            String contentMarkdown,
            Map<String, Object> metadataJson
    ) {
        return new GeneratedDocument(
                userId,
                roomId,
                suggestionId,
                resourceId,
                title,
                documentType,
                contentMarkdown,
                metadataJson
        );
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

    private static Map<String, Object> immutableJsonMap(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }
}
