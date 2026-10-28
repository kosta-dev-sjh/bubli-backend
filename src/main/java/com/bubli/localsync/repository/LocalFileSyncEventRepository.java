package com.bubli.localsync.repository;

import com.bubli.localsync.entity.LocalFileSyncEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface LocalFileSyncEventRepository extends JpaRepository<LocalFileSyncEvent, UUID> {

    Optional<LocalFileSyncEvent> findByUserIdAndLocalEventId(UUID userId, String localEventId);

    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO local_file_sync_events (
                id,
                user_id,
                local_event_id,
                event_type,
                resource_id,
                status,
                payload_fingerprint,
                retryable,
                created_at,
                updated_at
            )
            VALUES (
                :id,
                :userId,
                :localEventId,
                :eventType,
                NULL,
                'PROCESSING',
                :payloadFingerprint,
                TRUE,
                :now,
                :now
            )
            ON CONFLICT (user_id, local_event_id) DO NOTHING
            """, nativeQuery = true)
    int insertProcessingIfAbsent(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("localEventId") String localEventId,
            @Param("eventType") String eventType,
            @Param("now") Instant now,
            @Param("payloadFingerprint") String payloadFingerprint
    );

    default int insertProcessingIfAbsent(UUID id, UUID userId, String localEventId, String eventType, Instant now) {
        return insertProcessingIfAbsent(id, userId, localEventId, eventType, now, null);
    }

    @Transactional
    @Modifying
    @Query(value = """
            UPDATE local_file_sync_events
            SET event_type = :eventType,
                resource_id = :resourceId,
                status = :status,
                error_code = :errorCode,
                retryable = :retryable,
                updated_at = :now
            WHERE user_id = :userId
              AND local_event_id = :localEventId
              AND status = 'PROCESSING'
            """, nativeQuery = true)
    int updateResult(
            @Param("userId") UUID userId,
            @Param("localEventId") String localEventId,
            @Param("eventType") String eventType,
            @Param("resourceId") UUID resourceId,
            @Param("status") String status,
            @Param("now") Instant now,
            @Param("errorCode") String errorCode,
            @Param("retryable") boolean retryable
    );

    default int updateResult(UUID userId, String localEventId, String eventType, UUID resourceId,
                             String status, Instant now) {
        return updateResult(userId, localEventId, eventType, resourceId, status, now, null,
                "PROCESSING".equals(status) || "FAILED".equals(status));
    }

    @Transactional
    @Modifying
    @Query(value = """
            DELETE FROM local_file_sync_events
            WHERE user_id = :userId
              AND local_event_id = :localEventId
              AND status = 'PROCESSING'
            """, nativeQuery = true)
    int deleteProcessingEvent(
            @Param("userId") UUID userId,
            @Param("localEventId") String localEventId
    );
}
