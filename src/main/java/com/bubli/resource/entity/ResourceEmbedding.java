package com.bubli.resource.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.resource.type.ResourceVisibility;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "resource_embeddings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_resource_embeddings_resource_chunk",
                        columnNames = {"resource_id", "chunk_index"}
                )
        },
        indexes = {
                @Index(name = "idx_resource_embeddings_resource", columnList = "resource_id"),
                @Index(name = "idx_resource_embeddings_owner_visibility", columnList = "owner_id,visibility"),
                @Index(name = "idx_resource_embeddings_room_visibility", columnList = "room_id,visibility")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceEmbedding extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "room_id")
    private UUID roomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private ResourceVisibility visibility;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "text")
    private String chunkText;

    @Column(name = "embedding", nullable = false, columnDefinition = "vector(1024)")
    private String embedding;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "chunk_metadata", columnDefinition = "jsonb")
    private Map<String, Object> chunkMetadata;

    private ResourceEmbedding(
            UUID resourceId,
            UUID ownerId,
            UUID roomId,
            ResourceVisibility visibility,
            int chunkIndex,
            String chunkText,
            String embedding,
            Map<String, Object> chunkMetadata
    ) {
        this.resourceId = require(resourceId, "resourceId");
        this.ownerId = require(ownerId, "ownerId");
        this.roomId = roomId;
        this.visibility = require(visibility, "visibility");
        if (visibility == ResourceVisibility.ROOM_SHARED && roomId == null) {
            throw new IllegalArgumentException("ROOM_SHARED embeddings require roomId.");
        }
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("chunkIndex must not be negative.");
        }
        this.chunkIndex = chunkIndex;
        this.chunkText = requireText(chunkText, "chunkText");
        this.embedding = requireText(embedding, "embedding");
        this.chunkMetadata = immutableJsonMap(chunkMetadata);
    }

    public static ResourceEmbedding create(
            UUID resourceId,
            UUID ownerId,
            UUID roomId,
            ResourceVisibility visibility,
            int chunkIndex,
            String chunkText,
            String embedding,
            Map<String, Object> chunkMetadata
    ) {
        return new ResourceEmbedding(
                resourceId,
                ownerId,
                roomId,
                visibility,
                chunkIndex,
                chunkText,
                embedding,
                chunkMetadata
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
        return value;
    }

    private static Map<String, Object> immutableJsonMap(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }
}
