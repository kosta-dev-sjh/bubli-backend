package com.bubli.resource.dto;

import com.bubli.resource.entity.ResourceStorageDeleteRetryRecord;
import com.bubli.resource.type.ResourceStorageDeleteStatus;

import java.time.Instant;
import java.util.UUID;

public record ResourceStorageDeleteRetryRecordResult(
		UUID id,
		UUID resourceId,
		UUID fileId,
		String storageKey,
		ResourceStorageDeleteStatus status,
		int retryCount,
		String lastErrorMessage,
		Instant createdAt,
		Instant updatedAt
) {

	public static ResourceStorageDeleteRetryRecordResult from(ResourceStorageDeleteRetryRecord record) {
		return new ResourceStorageDeleteRetryRecordResult(
				record.getId(),
				record.getResourceId(),
				record.getFileId(),
				record.getStorageKey(),
				record.getStatus(),
				record.getRetryCount(),
				record.getLastErrorMessage(),
				record.getCreatedAt(),
				record.getUpdatedAt()
		);
	}
}
