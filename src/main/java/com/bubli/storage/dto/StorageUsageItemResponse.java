package com.bubli.storage.dto;

import com.bubli.storage.type.StorageScope;

import java.time.Instant;
import java.util.UUID;

public record StorageUsageItemResponse(
		UUID id,
		UUID userId,
		UUID roomId,
		StorageScope storageScope,
		long usedBytes,
		long limitBytes,
		long remainingBytes,
		Instant updatedAt
) {
	public static StorageUsageItemResponse from(StorageUsageResult result) {
		return new StorageUsageItemResponse(
				result.id(),
				result.userId(),
				result.roomId(),
				result.storageScope(),
				result.usedBytes(),
				result.limitBytes(),
				result.remainingBytes(),
				result.updatedAt()
		);
	}
}
