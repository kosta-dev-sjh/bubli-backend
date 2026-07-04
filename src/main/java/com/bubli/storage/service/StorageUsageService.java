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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageUsageService implements StorageUsagePublicService {

	private static final int STORAGE_USAGE_CREATE_MAX_ATTEMPTS = 3;

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
				.or(() -> java.util.Optional.of(defaultPersonalUsage(userId)))
				.ifPresent(usages::add);

		List<UUID> activeRoomIds = projectMembershipPublicService.findActiveRoomIds(userId);
		if (!activeRoomIds.isEmpty()) {
			storageUsageRepository.findByRoomIdInAndStorageScope(activeRoomIds, StorageScope.ROOM).stream()
					.map(StorageUsageResult::from)
					.forEach(usages::add);
		}

		return StorageUsageSummaryResult.from(usages);
	}

	private StorageUsageResult defaultPersonalUsage(UUID userId) {
		return new StorageUsageResult(
				null,
				userId,
				null,
				StorageScope.PERSONAL,
				0L,
				defaultPersonalLimitBytes,
				defaultPersonalLimitBytes,
				null
		);
	}

	@Transactional
	public StorageUsageResult recordPersonalUpload(UUID userId, long sizeBytes) {
		validateSizeBytes(sizeBytes);
		StorageUsage usage = getOrCreatePersonalUsageForUpdate(userId);
		increaseUsage(usage, sizeBytes);
		return StorageUsageResult.from(usage);
	}

	@Transactional
	public StorageUsageResult recordRoomUpload(UUID roomId, long sizeBytes) {
		validateSizeBytes(sizeBytes);
		StorageUsage usage = getOrCreateRoomUsageForUpdate(roomId);
		increaseUsage(usage, sizeBytes);
		return StorageUsageResult.from(usage);
	}

	@Transactional
	public StorageUsageResult releasePersonalUsage(UUID userId, long sizeBytes) {
		validateSizeBytes(sizeBytes);
		StorageUsage usage = getOrCreatePersonalUsageForUpdate(userId);
		usage.decreaseUsedBytes(sizeBytes);
		return StorageUsageResult.from(usage);
	}

	@Transactional
	public StorageUsageResult releaseRoomUsage(UUID roomId, long sizeBytes) {
		validateSizeBytes(sizeBytes);
		StorageUsage usage = getOrCreateRoomUsageForUpdate(roomId);
		usage.decreaseUsedBytes(sizeBytes);
		return StorageUsageResult.from(usage);
	}

	private StorageUsage getOrCreatePersonalUsageForUpdate(UUID userId) {
		return storageUsageRepository.findByUserIdAndStorageScopeForUpdate(userId, StorageScope.PERSONAL)
				.orElseGet(() -> createPersonalUsageWithRetry(userId));
	}

	private StorageUsage getOrCreateRoomUsageForUpdate(UUID roomId) {
		return storageUsageRepository.findByRoomIdAndStorageScopeForUpdate(roomId, StorageScope.ROOM)
				.orElseGet(() -> createRoomUsageWithRetry(roomId));
	}

	private StorageUsage createPersonalUsageWithRetry(UUID userId) {
		DataIntegrityViolationException lastException = null;
		for (int attempt = 0; attempt < STORAGE_USAGE_CREATE_MAX_ATTEMPTS; attempt++) {
			try {
				return storageUsageRepository.saveAndFlush(StorageUsage.create(
						userId,
						null,
						StorageScope.PERSONAL,
						0L,
						defaultPersonalLimitBytes
				));
			} catch (DataIntegrityViolationException exception) {
				StorageUsage existingUsage = storageUsageRepository
						.findByUserIdAndStorageScopeForUpdate(userId, StorageScope.PERSONAL)
						.orElse(null);
				if (existingUsage != null) {
					return existingUsage;
				}
				lastException = exception;
			}
		}
		if (lastException == null) {
			throw new IllegalStateException("Storage usage create retry attempts must be positive.");
		}
		throw lastException;
	}

	private StorageUsage createRoomUsageWithRetry(UUID roomId) {
		DataIntegrityViolationException lastException = null;
		for (int attempt = 0; attempt < STORAGE_USAGE_CREATE_MAX_ATTEMPTS; attempt++) {
			try {
				return storageUsageRepository.saveAndFlush(StorageUsage.create(
						null,
						roomId,
						StorageScope.ROOM,
						0L,
						defaultRoomLimitBytes
				));
			} catch (DataIntegrityViolationException exception) {
				StorageUsage existingUsage = storageUsageRepository
						.findByRoomIdAndStorageScopeForUpdate(roomId, StorageScope.ROOM)
						.orElse(null);
				if (existingUsage != null) {
					return existingUsage;
				}
				lastException = exception;
			}
		}
		if (lastException == null) {
			throw new IllegalStateException("Storage usage create retry attempts must be positive.");
		}
		throw lastException;
	}

	private void validateSizeBytes(long sizeBytes) {
		if (sizeBytes <= 0) {
			throw new BusinessException(ErrorCode.STORAGE_400_001);
		}
	}

	private void increaseUsage(StorageUsage usage, long sizeBytes) {
		long remainingBytes = usage.getLimitBytes() - usage.getUsedBytes();
		if (remainingBytes < 0 || sizeBytes > remainingBytes) {
			throw new BusinessException(ErrorCode.STORAGE_400_002);
		}
		usage.increaseUsedBytes(sizeBytes);
	}
}
