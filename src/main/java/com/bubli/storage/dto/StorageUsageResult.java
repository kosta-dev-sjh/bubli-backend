package com.bubli.storage.dto;

import com.bubli.storage.entity.StorageUsage;
import com.bubli.storage.type.StorageScope;

import java.time.Instant;
import java.util.UUID;

public record StorageUsageResult(
		UUID id,
		UUID userId,
		UUID roomId,
		StorageScope storageScope,
		long usedBytes,
		long limitBytes,
		long remainingBytes,
		Instant updatedAt
) {
	public static StorageUsageResult from(StorageUsage storageUsage) {
		long remainingBytes = Math.max(storageUsage.getLimitBytes() - storageUsage.getUsedBytes(), 0L);
		return new StorageUsageResult(
				storageUsage.getId(),
				storageUsage.getUserId(),
				storageUsage.getRoomId(),
				storageUsage.getStorageScope(),
				storageUsage.getUsedBytes(),
				storageUsage.getLimitBytes(),
				remainingBytes,
				storageUsage.getUpdatedAt()
		);
	}
}
