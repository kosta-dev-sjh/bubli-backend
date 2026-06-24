package com.bubli.storage.dto;

import java.util.List;

public record StorageUsageResponse(
		List<StorageUsageItemResponse> usages,
		long totalUsedBytes,
		long totalLimitBytes,
		long totalRemainingBytes
) {
	public static StorageUsageResponse from(StorageUsageSummaryResult result) {
		return new StorageUsageResponse(
				result.usages().stream()
						.map(StorageUsageItemResponse::from)
						.toList(),
				result.totalUsedBytes(),
				result.totalLimitBytes(),
				result.totalRemainingBytes()
		);
	}
}
