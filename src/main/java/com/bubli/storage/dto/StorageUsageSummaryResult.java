package com.bubli.storage.dto;

import java.util.List;

public record StorageUsageSummaryResult(
		List<StorageUsageResult> usages,
		long totalUsedBytes,
		long totalLimitBytes,
		long totalRemainingBytes
) {
	public static StorageUsageSummaryResult from(List<StorageUsageResult> usages) {
		long totalUsedBytes = usages.stream().mapToLong(StorageUsageResult::usedBytes).sum();
		long totalLimitBytes = usages.stream().mapToLong(StorageUsageResult::limitBytes).sum();
		long totalRemainingBytes = usages.stream().mapToLong(StorageUsageResult::remainingBytes).sum();
		return new StorageUsageSummaryResult(usages, totalUsedBytes, totalLimitBytes, totalRemainingBytes);
	}
}
