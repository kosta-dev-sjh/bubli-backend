package com.bubli.storage.service;

import com.bubli.project.entity.RoomMember;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.RoomMemberStatus;
import com.bubli.storage.entity.StorageUsage;
import com.bubli.storage.repository.StorageUsageRepository;
import com.bubli.storage.type.StorageScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StorageUsageServiceTest {

	@Mock
	StorageUsageRepository storageUsageRepository;

	@Mock
	RoomMemberRepository roomMemberRepository;

	@InjectMocks
	StorageUsageService storageUsageService;

	@Test
	void getMyStorageUsageReturnsPersonalAndActiveRoomUsage() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		StorageUsage personalUsage = storageUsage(userId, null, StorageScope.PERSONAL, 100L, 1000L);
		StorageUsage roomUsage = storageUsage(null, roomId, StorageScope.ROOM, 300L, 2000L);
		RoomMember roomMember = RoomMember.createMember(roomId, userId);

		given(storageUsageRepository.findByUserIdAndStorageScope(userId, StorageScope.PERSONAL))
				.willReturn(Optional.of(personalUsage));
		given(roomMemberRepository.findByUserIdAndStatus(userId, RoomMemberStatus.ACTIVE))
				.willReturn(List.of(roomMember));
		given(storageUsageRepository.findByRoomIdInAndStorageScope(List.of(roomId), StorageScope.ROOM))
				.willReturn(List.of(roomUsage));

		var result = storageUsageService.getMyStorageUsage(userId);

		assertThat(result.usages()).hasSize(2);
		assertThat(result.totalUsedBytes()).isEqualTo(400L);
		assertThat(result.totalLimitBytes()).isEqualTo(3000L);
		assertThat(result.totalRemainingBytes()).isEqualTo(2600L);
	}

	@Test
	void getMyStorageUsageReturnsEmptySummaryWhenNoRowsExist() {
		UUID userId = UUID.randomUUID();
		given(storageUsageRepository.findByUserIdAndStorageScope(userId, StorageScope.PERSONAL))
				.willReturn(Optional.empty());
		given(roomMemberRepository.findByUserIdAndStatus(userId, RoomMemberStatus.ACTIVE))
				.willReturn(List.of());

		var result = storageUsageService.getMyStorageUsage(userId);

		assertThat(result.usages()).isEmpty();
		assertThat(result.totalUsedBytes()).isZero();
		assertThat(result.totalLimitBytes()).isZero();
		assertThat(result.totalRemainingBytes()).isZero();
	}

	private StorageUsage storageUsage(
			UUID userId,
			UUID roomId,
			StorageScope storageScope,
			long usedBytes,
			long limitBytes
	) {
		StorageUsage storageUsage = StorageUsage.create(userId, roomId, storageScope, usedBytes, limitBytes);
		ReflectionTestUtils.setField(storageUsage, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(storageUsage, "updatedAt", Instant.now());
		return storageUsage;
	}
}
