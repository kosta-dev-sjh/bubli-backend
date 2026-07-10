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
            @Param("now") Instant now
    );

    @Transactional
    @Modifying
    @Query(value = """
            UPDATE local_file_sync_events
            SET event_type = :eventType,
                resource_id = :resourceId,
                status = :status,
                updated_at = :now
            WHERE user_id = :userId
              AND local_event_id = :localEventId
            """, nativeQuery = true)
    int updateResult(
            @Param("userId") UUID userId,
            @Param("localEventId") String localEventId,
            @Param("eventType") String eventType,
            @Param("resourceId") UUID resourceId,
            @Param("status") String status,
            @Param("now") Instant now
    );

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
