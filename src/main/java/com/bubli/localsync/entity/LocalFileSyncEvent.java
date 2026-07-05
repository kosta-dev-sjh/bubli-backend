package com.bubli.localsync.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "local_file_sync_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_local_file_sync_events_user_event", columnNames = {"user_id", "local_event_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocalFileSyncEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "local_event_id", nullable = false, length = 120)
    private String localEventId;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static LocalFileSyncEvent create(UUID userId, String localEventId, String eventType, UUID resourceId, String status) {
        LocalFileSyncEvent event = new LocalFileSyncEvent();
        event.userId = userId;
        event.localEventId = localEventId;
        event.eventType = eventType;
        event.resourceId = resourceId;
        event.status = status;
        return event;
    }

    @PrePersist
    private void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
