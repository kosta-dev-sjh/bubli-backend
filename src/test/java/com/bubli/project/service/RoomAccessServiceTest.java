package com.bubli.project.service;

import com.bubli.global.error.BusinessException;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.RoomMemberRole;
import com.bubli.project.type.RoomMemberStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RoomAccessServiceTest {

	@Mock
	RoomMemberRepository roomMemberRepository;

	@InjectMocks
	RoomAccessService roomAccessService;

	@Test
	void isActiveMemberChecksActiveRoomMember() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		given(roomMemberRepository.existsByRoomIdAndUserIdAndStatus(
				roomId,
				userId,
				RoomMemberStatus.ACTIVE
		)).willReturn(true);

		boolean result = roomAccessService.isActiveMember(userId, roomId);

		assertThat(result).isTrue();
	}

	@Test
	void validateActiveMemberRejectsInactiveMember() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		given(roomMemberRepository.existsByRoomIdAndUserIdAndStatus(
				roomId,
				userId,
				RoomMemberStatus.ACTIVE
		)).willReturn(false);

		assertThatThrownBy(() -> roomAccessService.validateActiveMember(userId, roomId))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void isProjectLeaderChecksActiveLeaderMember() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		given(roomMemberRepository.existsByRoomIdAndUserIdAndStatusAndRole(
				roomId,
				userId,
				RoomMemberStatus.ACTIVE,
				RoomMemberRole.PROJECT_LEADER
		)).willReturn(true);

		boolean result = roomAccessService.isProjectLeader(userId, roomId);

		assertThat(result).isTrue();
	}

	@Test
	void validateProjectLeaderRejectsNonLeader() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		given(roomMemberRepository.existsByRoomIdAndUserIdAndStatusAndRole(
				roomId,
				userId,
				RoomMemberStatus.ACTIVE,
				RoomMemberRole.PROJECT_LEADER
		)).willReturn(false);

		assertThatThrownBy(() -> roomAccessService.validateProjectLeader(userId, roomId))
				.isInstanceOf(BusinessException.class);
	}
}
