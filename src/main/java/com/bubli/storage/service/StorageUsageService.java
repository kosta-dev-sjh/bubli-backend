package com.bubli.storage.service;

import com.bubli.project.entity.RoomMember;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.RoomMemberStatus;
import com.bubli.storage.dto.StorageUsageResult;
import com.bubli.storage.dto.StorageUsageSummaryResult;
import com.bubli.storage.repository.StorageUsageRepository;
import com.bubli.storage.type.StorageScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageUsageService {

	private final StorageUsageRepository storageUsageRepository;
	private final RoomMemberRepository roomMemberRepository;

	@Transactional(readOnly = true)
	public StorageUsageSummaryResult getMyStorageUsage(UUID userId) {
		List<StorageUsageResult> usages = new ArrayList<>();
		storageUsageRepository.findByUserIdAndStorageScope(userId, StorageScope.PERSONAL)
				.map(StorageUsageResult::from)
				.ifPresent(usages::add);

		List<UUID> activeRoomIds = roomMemberRepository.findByUserIdAndStatus(userId, RoomMemberStatus.ACTIVE).stream()
				.map(RoomMember::getRoomId)
				.toList();
		if (!activeRoomIds.isEmpty()) {
			storageUsageRepository.findByRoomIdInAndStorageScope(activeRoomIds, StorageScope.ROOM).stream()
					.map(StorageUsageResult::from)
					.forEach(usages::add);
		}

		return StorageUsageSummaryResult.from(usages);
	}
}
