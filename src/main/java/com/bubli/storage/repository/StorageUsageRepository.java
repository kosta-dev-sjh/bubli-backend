package com.bubli.storage.repository;

import com.bubli.storage.entity.StorageUsage;
import com.bubli.storage.type.StorageScope;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorageUsageRepository extends JpaRepository<StorageUsage, UUID> {

	Optional<StorageUsage> findByUserIdAndStorageScope(UUID userId, StorageScope storageScope);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select usage
			from StorageUsage usage
			where usage.userId = :userId
			  and usage.storageScope = :storageScope
			""")
	Optional<StorageUsage> findByUserIdAndStorageScopeForUpdate(
			@Param("userId") UUID userId,
			@Param("storageScope") StorageScope storageScope
	);

	Optional<StorageUsage> findByRoomIdAndStorageScope(UUID roomId, StorageScope storageScope);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select usage
			from StorageUsage usage
			where usage.roomId = :roomId
			  and usage.storageScope = :storageScope
			""")
	Optional<StorageUsage> findByRoomIdAndStorageScopeForUpdate(
			@Param("roomId") UUID roomId,
			@Param("storageScope") StorageScope storageScope
	);

	List<StorageUsage> findByRoomIdInAndStorageScope(List<UUID> roomIds, StorageScope storageScope);
}
