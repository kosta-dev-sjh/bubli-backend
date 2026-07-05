package com.bubli.localsync.repository;

import com.bubli.localsync.entity.LocalFileSyncEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LocalFileSyncEventRepository extends JpaRepository<LocalFileSyncEvent, UUID> {

    Optional<LocalFileSyncEvent> findByUserIdAndLocalEventId(UUID userId, String localEventId);
}
