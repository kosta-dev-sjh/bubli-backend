package com.bubli.storage.repository;

import com.bubli.storage.entity.StorageUsage;
import com.bubli.storage.type.StorageScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorageUsageRepository extends JpaRepository<StorageUsage, UUID> {

	Optional<StorageUsage> findByUserIdAndStorageScope(UUID userId, StorageScope storageScope);

	Optional<StorageUsage> findByRoomIdAndStorageScope(UUID roomId, StorageScope storageScope);

	List<StorageUsage> findByRoomIdInAndStorageScope(List<UUID> roomIds, StorageScope storageScope);
}
