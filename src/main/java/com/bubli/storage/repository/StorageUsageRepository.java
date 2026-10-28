package com.bubli.storage.repository;

import com.bubli.storage.entity.StorageUsage;
import com.bubli.storage.type.StorageScope;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;

public interface StorageUsageRepository extends JpaRepository<StorageUsage, UUID> {

	@Transactional
	@Modifying
	@Query(value = """
			INSERT INTO storage_usage (id,user_id,room_id,storage_scope,used_bytes,limit_bytes,created_at,updated_at)
			VALUES (:id,:userId,NULL,'PERSONAL',0,:limitBytes,:now,:now)
			ON CONFLICT (user_id) WHERE storage_scope = 'PERSONAL' AND user_id IS NOT NULL DO NOTHING
			""", nativeQuery = true)
	int insertPersonalIfAbsent(@Param("id") UUID id, @Param("userId") UUID userId,
			@Param("limitBytes") long limitBytes, @Param("now") Instant now);

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
