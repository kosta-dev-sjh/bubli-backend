package com.bubli.resource.entity;

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

import java.util.UUID;

/**
 * 프로젝트룸 자료 버전 기록.
 *
 * 테이블: resource_versions
 * 주요 필드: resource_id, version_no, file_id, created_by
 *
 * 같은 자료를 재업로드하면 기존 파일을 덮어쓰지 않고 새 버전으로 저장한다.
 * 최신 version만 기본 표시하고, 이전 버전은 버전 목록에서 선택.
 */
@Entity
@Table(
        name = "resource_versions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_resource_versions_resource_version",
                        columnNames = {"resource_id", "version_no"}
                )
        },
        indexes = {
                @Index(name = "idx_resource_versions_resource", columnList = "resource_id"),
                @Index(name = "idx_resource_versions_file", columnList = "file_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceVersion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    private ResourceVersion(UUID resourceId, int versionNo, UUID fileId, UUID createdBy) {
        this.resourceId = require(resourceId, "resourceId");
        if (versionNo < 1) {
            throw new IllegalArgumentException("versionNo must be greater than zero.");
        }
        this.versionNo = versionNo;
        this.fileId = require(fileId, "fileId");
        this.createdBy = require(createdBy, "createdBy");
    }

    public static ResourceVersion first(UUID resourceId, UUID fileId, UUID createdBy) {
        return new ResourceVersion(resourceId, 1, fileId, createdBy);
    }

    public static ResourceVersion create(UUID resourceId, int versionNo, UUID fileId, UUID createdBy) {
        return new ResourceVersion(resourceId, versionNo, fileId, createdBy);
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value;
    }
}
