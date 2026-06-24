package com.bubli.storage.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.entity.RoomMember;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.RoomMemberStatus;
import com.bubli.storage.entity.StorageUsage;
import com.bubli.storage.repository.StorageUsageRepository;
import com.bubli.storage.type.StorageScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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

	@Test
	void recordPersonalUploadCreatesDefaultUsageAndIncreasesUsedBytes() {
		UUID userId = UUID.randomUUID();
		ReflectionTestUtils.setField(storageUsageService, "defaultPersonalLimitBytes", 1000L);
		given(storageUsageRepository.findByUserIdAndStorageScope(userId, StorageScope.PERSONAL))
				.willReturn(Optional.empty());
		given(storageUsageRepository.save(any(StorageUsage.class))).willAnswer(invocation -> {
			StorageUsage usage = invocation.getArgument(0);
			ReflectionTestUtils.setField(usage, "id", UUID.randomUUID());
			ReflectionTestUtils.setField(usage, "updatedAt", Instant.now());
			return usage;
		});

		var result = storageUsageService.recordPersonalUpload(userId, 300L);

		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.storageScope()).isEqualTo(StorageScope.PERSONAL);
		assertThat(result.usedBytes()).isEqualTo(300L);
		assertThat(result.limitBytes()).isEqualTo(1000L);
		assertThat(result.remainingBytes()).isEqualTo(700L);

		ArgumentCaptor<StorageUsage> usageCaptor = ArgumentCaptor.forClass(StorageUsage.class);
		verify(storageUsageRepository).save(usageCaptor.capture());
		assertThat(usageCaptor.getValue().getUsedBytes()).isEqualTo(300L);
	}

	@Test
	void recordRoomUploadRejectsWhenLimitWouldBeExceeded() {
		UUID roomId = UUID.randomUUID();
		StorageUsage usage = storageUsage(null, roomId, StorageScope.ROOM, 900L, 1000L);
		given(storageUsageRepository.findByRoomIdAndStorageScope(roomId, StorageScope.ROOM))
				.willReturn(Optional.of(usage));

		assertThatThrownBy(() -> storageUsageService.recordRoomUpload(roomId, 200L))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STORAGE_400_002));
		assertThat(usage.getUsedBytes()).isEqualTo(900L);
	}

	@Test
	void releaseRoomUsageDoesNotGoBelowZero() {
		UUID roomId = UUID.randomUUID();
		StorageUsage usage = storageUsage(null, roomId, StorageScope.ROOM, 100L, 1000L);
		given(storageUsageRepository.findByRoomIdAndStorageScope(roomId, StorageScope.ROOM))
				.willReturn(Optional.of(usage));

		var result = storageUsageService.releaseRoomUsage(roomId, 300L);

		assertThat(result.usedBytes()).isZero();
		assertThat(result.remainingBytes()).isEqualTo(1000L);
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
