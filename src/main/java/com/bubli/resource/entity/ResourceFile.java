package com.bubli.resource.entity;

import com.bubli.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "resource_files",
        indexes = {
                @Index(name = "idx_resource_files_resource", columnList = "resource_id"),
                @Index(name = "idx_resource_files_checksum", columnList = "checksum")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceFile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_path", nullable = false, length = 1000)
    private String storagePath;

    @Column(name = "checksum", length = 64)
    private String checksum;

    private ResourceFile(
            UUID resourceId,
            String originalName,
            String mimeType,
            long sizeBytes,
            String storagePath,
            String checksum
    ) {
        this.resourceId = require(resourceId, "resourceId");
        this.originalName = requireText(originalName, "originalName");
        this.mimeType = requireText(mimeType, "mimeType");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative.");
        }
        this.sizeBytes = sizeBytes;
        this.storagePath = requireText(storagePath, "storagePath");
        this.checksum = checksum;
    }

    public static ResourceFile create(
            UUID resourceId,
            String originalName,
            String mimeType,
            long sizeBytes,
            String storagePath,
            String checksum
    ) {
        return new ResourceFile(resourceId, originalName, mimeType, sizeBytes, storagePath, checksum);
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
}
