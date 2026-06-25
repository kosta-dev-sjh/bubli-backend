package com.bubli.storage.service;

import com.bubli.storage.dto.StorageUsageResult;

import java.util.UUID;

public interface StorageUsagePublicService {

	StorageUsageResult recordPersonalUpload(UUID userId, long sizeBytes);

	StorageUsageResult recordRoomUpload(UUID roomId, long sizeBytes);

	StorageUsageResult releasePersonalUsage(UUID userId, long sizeBytes);

	StorageUsageResult releaseRoomUsage(UUID roomId, long sizeBytes);
}
