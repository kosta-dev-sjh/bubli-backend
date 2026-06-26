package com.bubli.storage.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.storage.dto.StorageUsageResult;
import com.bubli.storage.dto.StorageUsageSummaryResult;
import com.bubli.storage.entity.StorageUsage;
import com.bubli.storage.repository.StorageUsageRepository;
import com.bubli.storage.type.StorageScope;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageUsageService implements StorageUsagePublicService {

	private final StorageUsageRepository storageUsageRepository;
	private final ProjectMembershipPublicService projectMembershipPublicService;

	@Value("${storage.default-personal-limit-bytes:1073741824}")
	private long defaultPersonalLimitBytes = 1_073_741_824L;

	@Value("${storage.default-room-limit-bytes:5368709120}")
	private long defaultRoomLimitBytes = 5_368_709_120L;

	@Transactional(readOnly = true)
	public StorageUsageSummaryResult getMyStorageUsage(UUID userId) {
		List<StorageUsageResult> usages = new ArrayList<>();
		storageUsageRepository.findByUserIdAndStorageScope(userId, StorageScope.PERSONAL)
				.map(StorageUsageResult::from)
				.ifPresent(usages::add);

		List<UUID> activeRoomIds = projectMembershipPublicService.findActiveRoomIds(userId);
		if (!activeRoomIds.isEmpty()) {
			storageUsageRepository.findByRoomIdInAndStorageScope(activeRoomIds, StorageScope.ROOM).stream()
					.map(StorageUsageResult::from)
					.forEach(usages::add);
		}

		return StorageUsageSummaryResult.from(usages);
	}

	@Transactional
	public StorageUsageResult recordPersonalUpload(UUID userId, long sizeBytes) {
		validateSizeBytes(sizeBytes);
		StorageUsage usage = storageUsageRepository.findByUserIdAndStorageScope(userId, StorageScope.PERSONAL)
				.orElseGet(() -> storageUsageRepository.save(StorageUsage.create(
						userId,
						null,
						StorageScope.PERSONAL,
						0L,
						defaultPersonalLimitBytes
				)));
		increaseUsage(usage, sizeBytes);
		return StorageUsageResult.from(usage);
	}

	@Transactional
	public StorageUsageResult recordRoomUpload(UUID roomId, long sizeBytes) {
		validateSizeBytes(sizeBytes);
		StorageUsage usage = storageUsageRepository.findByRoomIdAndStorageScope(roomId, StorageScope.ROOM)
				.orElseGet(() -> storageUsageRepository.save(StorageUsage.create(
						null,
						roomId,
						StorageScope.ROOM,
						0L,
						defaultRoomLimitBytes
				)));
		increaseUsage(usage, sizeBytes);
		return StorageUsageResult.from(usage);
	}

	@Transactional
	public StorageUsageResult releasePersonalUsage(UUID userId, long sizeBytes) {
		validateSizeBytes(sizeBytes);
		StorageUsage usage = storageUsageRepository.findByUserIdAndStorageScope(userId, StorageScope.PERSONAL)
				.orElseGet(() -> storageUsageRepository.save(StorageUsage.create(
						userId,
						null,
						StorageScope.PERSONAL,
						0L,
						defaultPersonalLimitBytes
				)));
		usage.decreaseUsedBytes(sizeBytes);
		return StorageUsageResult.from(usage);
	}

	@Transactional
	public StorageUsageResult releaseRoomUsage(UUID roomId, long sizeBytes) {
		validateSizeBytes(sizeBytes);
		StorageUsage usage = storageUsageRepository.findByRoomIdAndStorageScope(roomId, StorageScope.ROOM)
				.orElseGet(() -> storageUsageRepository.save(StorageUsage.create(
						null,
						roomId,
						StorageScope.ROOM,
						0L,
						defaultRoomLimitBytes
				)));
		usage.decreaseUsedBytes(sizeBytes);
		return StorageUsageResult.from(usage);
	}

	private void validateSizeBytes(long sizeBytes) {
		if (sizeBytes <= 0) {
			throw new BusinessException(ErrorCode.STORAGE_400_001);
		}
	}

	private void increaseUsage(StorageUsage usage, long sizeBytes) {
		if (usage.getUsedBytes() + sizeBytes > usage.getLimitBytes()) {
			throw new BusinessException(ErrorCode.STORAGE_400_002);
		}
		usage.increaseUsedBytes(sizeBytes);
	}
}
