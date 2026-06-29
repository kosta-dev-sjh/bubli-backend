package com.bubli.resource.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceStatus;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * 개인 자료 또는 프로젝트룸 자료 카드.
 *
 * 테이블: resources
 * 주요 필드: owner_id, project_id, room_id, title, kind, visibility, status
 *
 * visibility: PERSONAL(owner만 접근) / ROOM_SHARED(room_members 접근)
 * status: UPLOADING → READY → ANALYZING → ANALYZED / FAILED / DELETE_CANDIDATE / ARCHIVED
 *
 * PERSONAL 자료는 프로젝트룸 멤버에게 보이지 않으며,
 * 프로젝트룸 에이전트 context에 포함되지 않는다.
 */
@Entity
@Table(
        name = "resources",
        indexes = {
                @Index(name = "idx_resources_owner_visibility", columnList = "owner_id,visibility"),
                @Index(name = "idx_resources_room_status", columnList = "room_id,status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resource extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private ResourceKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private ResourceVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ResourceStatus status;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private Resource(
            UUID ownerId,
            UUID roomId,
            String title,
            ResourceKind kind,
            ResourceVisibility visibility
    ) {
        this.ownerId = require(ownerId, "ownerId");
        this.roomId = roomId;
        this.title = requireText(title, "title");
        this.kind = require(kind, "kind");
        this.visibility = require(visibility, "visibility");
        if (visibility == ResourceVisibility.ROOM_SHARED && roomId == null) {
            throw new IllegalArgumentException("ROOM_SHARED 자료에는 roomId가 필요합니다.");
        }
        this.status = ResourceStatus.UPLOADING;
    }

    public static Resource roomFile(UUID ownerId, UUID roomId, String title) {
        return new Resource(ownerId, roomId, title, ResourceKind.FILE, ResourceVisibility.ROOM_SHARED);
    }

    public static Resource create(
            UUID ownerId,
            UUID roomId,
            String title,
            ResourceKind kind,
            ResourceVisibility visibility,
            ResourceStatus status
    ) {
        Resource resource = new Resource(ownerId, roomId, title, kind, visibility);
        resource.status = require(status, "status");
        return resource;
    }

    public void updateTitle(String title) {
        this.title = requireText(title, "title");
    }

    public void markDeleted(Instant deletedAt) {
        this.deletedAt = require(deletedAt, "deletedAt");
    }

    public void markReady() {
        status = ResourceStatus.READY;
    }

    public void startAnalysis() {
        status = ResourceStatus.ANALYZING;
    }

    public void markAnalysisFailed() {
        status = ResourceStatus.FAILED;
    }

    public void markAnalyzed() {
        status = ResourceStatus.ANALYZED;
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
