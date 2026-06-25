package com.bubli.resource.dto;

import com.bubli.resource.entity.ResourceStorageDeleteRequest;
import com.bubli.resource.type.ResourceStorageDeleteStatus;

import java.time.Instant;
import java.util.UUID;

public record ResourceStorageDeleteRequestResult(
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

	public static ResourceStorageDeleteRequestResult from(ResourceStorageDeleteRequest request) {
		return new ResourceStorageDeleteRequestResult(
				request.getId(),
				request.getResourceId(),
				request.getFileId(),
				request.getStorageKey(),
				request.getStatus(),
				request.getRetryCount(),
				request.getLastErrorMessage(),
				request.getCreatedAt(),
				request.getUpdatedAt()
		);
	}
}
