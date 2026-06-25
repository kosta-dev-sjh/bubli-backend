package com.bubli.project.service;

import com.bubli.global.error.BusinessException;
import com.bubli.project.entity.RoomMember;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.RoomMemberRole;
import com.bubli.project.type.RoomMemberStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProjectMembershipPublicServiceTest {

	@Mock
	RoomMemberRepository roomMemberRepository;

	@InjectMocks
	ProjectMembershipPublicService projectMembershipPublicService;

	@Test
	void assertActiveMemberPassesForActiveRoomMember() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		given(roomMemberRepository.existsByRoomIdAndUserIdAndStatus(roomId, userId, RoomMemberStatus.ACTIVE))
				.willReturn(true);

		projectMembershipPublicService.assertActiveMember(userId, roomId);
	}

	@Test
	void assertActiveMemberRejectsNonMember() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		given(roomMemberRepository.existsByRoomIdAndUserIdAndStatus(roomId, userId, RoomMemberStatus.ACTIVE))
				.willReturn(false);

		assertThatThrownBy(() -> projectMembershipPublicService.assertActiveMember(userId, roomId))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void findActiveRoomIdsReturnsOnlyRepositoryActiveMemberships() {
		UUID userId = UUID.randomUUID();
		UUID firstRoomId = UUID.randomUUID();
		UUID secondRoomId = UUID.randomUUID();
		given(roomMemberRepository.findByUserIdAndStatus(userId, RoomMemberStatus.ACTIVE))
				.willReturn(List.of(
						RoomMember.create(firstRoomId, userId, RoomMemberRole.PROJECT_LEADER),
						RoomMember.create(secondRoomId, userId, RoomMemberRole.MEMBER)
				));

		assertThat(projectMembershipPublicService.findActiveRoomIds(userId))
				.containsExactly(firstRoomId, secondRoomId);
	}
}
