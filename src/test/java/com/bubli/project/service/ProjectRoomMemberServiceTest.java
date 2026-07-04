package com.bubli.project.service;

import com.bubli.chat.service.RoomChatPublicService;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.dto.CreateInvitationCommand;
import com.bubli.project.dto.InvitationResult;
import com.bubli.project.entity.Invitation;
import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.entity.RoomMember;
import com.bubli.project.repository.InvitationRepository;
import com.bubli.project.repository.ProjectRoomRepository;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.InvitationStatus;
import com.bubli.project.type.RoomMemberRole;
import com.bubli.project.type.RoomMemberStatus;
import com.bubli.user.dto.UserResult;
import com.bubli.user.entity.User;
import com.bubli.user.service.FriendshipPublicService;
import com.bubli.user.service.UserPublicService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectRoomMemberServiceTest {

	@Mock
	RoomMemberRepository roomMemberRepository;

	@Mock
	ProjectRoomRepository projectRoomRepository;

	@Mock
	InvitationRepository invitationRepository;

	@Mock
	UserPublicService userPublicService;

	@Mock
	FriendshipPublicService friendshipPublicService;

	@Mock
	ProjectMembershipPublicService projectMembershipPublicService;

	@Mock
	RoomChatPublicService roomChatPublicService;

	@InjectMocks
	ProjectRoomMemberService projectRoomMemberService;

	@Test
	void projectLeaderCanCreateInvitation() {
		UUID roomId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		User invitee = user(UUID.randomUUID(), "invitee", "준화");
		CreateInvitationCommand command = new CreateInvitationCommand(invitee.getId(), RoomMemberRole.MEMBER, null);

		given(userPublicService.getUser(invitee.getId())).willReturn(userResult(invitee));
		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, invitee.getId(), RoomMemberStatus.ACTIVE))
				.willReturn(Optional.empty());
		given(invitationRepository.existsByRoomIdAndInviteeUserIdAndStatus(
				roomId,
				invitee.getId(),
				InvitationStatus.PENDING
		)).willReturn(false);
		given(invitationRepository.insertPendingIfAbsent(
				any(UUID.class),
				org.mockito.ArgumentMatchers.eq(roomId),
				org.mockito.ArgumentMatchers.eq(leaderId),
				org.mockito.ArgumentMatchers.eq(invitee.getId()),
				org.mockito.ArgumentMatchers.eq(RoomMemberRole.MEMBER.name()),
				any(Instant.class)
		)).willReturn(1);
		given(invitationRepository.findById(any(UUID.class))).willAnswer(invocation -> {
			UUID invitationId = invocation.getArgument(0);
			Invitation invitation = Invitation.create(
					roomId,
					leaderId,
					invitee.getId(),
					RoomMemberRole.MEMBER,
					Instant.now().plusSeconds(3600)
			);
			ReflectionTestUtils.setField(invitation, "id", invitationId);
			return Optional.of(invitation);
		});

		InvitationResult result = projectRoomMemberService.createInvitation(leaderId, roomId, command);

		assertThat(result.roomId()).isEqualTo(roomId);
		assertThat(result.inviteeUserId()).isEqualTo(invitee.getId());
		assertThat(result.inviteeName()).isEqualTo("준화");
		assertThat(result.role()).isEqualTo(RoomMemberRole.MEMBER);
		assertThat(result.status()).isEqualTo(InvitationStatus.PENDING);
	}

	@Test
	void createInvitationRequiresAcceptedFriendship() {
		UUID roomId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		User invitee = user(UUID.randomUUID(), "invitee", "준화");
		CreateInvitationCommand command = new CreateInvitationCommand(invitee.getId(), RoomMemberRole.MEMBER, null);

		given(userPublicService.getUser(invitee.getId())).willReturn(userResult(invitee));
		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, invitee.getId(), RoomMemberStatus.ACTIVE))
				.willReturn(Optional.empty());
		willThrow(new BusinessException(ErrorCode.PROJECT_403_003))
				.given(friendshipPublicService)
				.assertAcceptedFriend(leaderId, invitee.getId());

		assertThatThrownBy(() -> projectRoomMemberService.createInvitation(leaderId, roomId, command))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_403_003));
	}

	@Test
	void createInvitationReturnsConflictWhenPendingInsertIsSkippedByUniqueIndex() {
		UUID roomId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		User invitee = user(UUID.randomUUID(), "invitee", "준화");
		CreateInvitationCommand command = new CreateInvitationCommand(invitee.getId(), RoomMemberRole.MEMBER, null);

		given(userPublicService.getUser(invitee.getId())).willReturn(userResult(invitee));
		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, invitee.getId(), RoomMemberStatus.ACTIVE))
				.willReturn(Optional.empty());
		given(invitationRepository.existsByRoomIdAndInviteeUserIdAndStatus(
				roomId,
				invitee.getId(),
				InvitationStatus.PENDING
		)).willReturn(false);
		given(invitationRepository.insertPendingIfAbsent(
				any(UUID.class),
				org.mockito.ArgumentMatchers.eq(roomId),
				org.mockito.ArgumentMatchers.eq(leaderId),
				org.mockito.ArgumentMatchers.eq(invitee.getId()),
				org.mockito.ArgumentMatchers.eq(RoomMemberRole.MEMBER.name()),
				any(Instant.class)
		)).willReturn(0);

		assertThatThrownBy(() -> projectRoomMemberService.createInvitation(leaderId, roomId, command))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_409_003));
	}

	@Test
	void createInvitationRejectsAlreadyExpiredExpiresAt() {
		UUID roomId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		User invitee = user(UUID.randomUUID(), "invitee", "준화");
		CreateInvitationCommand command = new CreateInvitationCommand(
				invitee.getId(),
				RoomMemberRole.MEMBER,
				Instant.now().minusSeconds(60)
		);

		given(userPublicService.getUser(invitee.getId())).willReturn(userResult(invitee));
		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, invitee.getId(), RoomMemberStatus.ACTIVE))
				.willReturn(Optional.empty());
		given(invitationRepository.existsByRoomIdAndInviteeUserIdAndStatus(
				roomId,
				invitee.getId(),
				InvitationStatus.PENDING
		)).willReturn(false);

		assertThatThrownBy(() -> projectRoomMemberService.createInvitation(leaderId, roomId, command))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_400_002));
	}

	@Test
	void ordinaryMemberCannotCreateInvitation() {
		UUID roomId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		User invitee = user(UUID.randomUUID(), "invitee", "준화");
		CreateInvitationCommand command = new CreateInvitationCommand(invitee.getId(), RoomMemberRole.MEMBER, null);

		willThrow(new BusinessException(ErrorCode.PROJECT_403_002))
				.given(projectMembershipPublicService)
				.assertProjectLeader(memberId, roomId);

		assertThatThrownBy(() -> projectRoomMemberService.createInvitation(memberId, roomId, command))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_403_002));
	}

	@Test
	void cannotCreateInvitationForAlreadyActiveMember() {
		UUID roomId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		User invitee = user(UUID.randomUUID(), "invitee", "준화");
		RoomMember activeMember = RoomMember.createMember(roomId, invitee.getId());
		CreateInvitationCommand command = new CreateInvitationCommand(invitee.getId(), RoomMemberRole.MEMBER, null);

		given(userPublicService.getUser(invitee.getId())).willReturn(userResult(invitee));
		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, invitee.getId(), RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(activeMember));

		assertThatThrownBy(() -> projectRoomMemberService.createInvitation(leaderId, roomId, command))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_409_001));
	}

	@Test
	void inviteeCanAcceptInvitationAndBecomeRoomMember() {
		UUID roomId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		User invitee = user(UUID.randomUUID(), "invitee", "민서");
		Invitation invitation = Invitation.create(
				roomId,
				leaderId,
				invitee.getId(),
				RoomMemberRole.MEMBER,
				Instant.now().plusSeconds(3600)
		);
		ReflectionTestUtils.setField(invitation, "id", UUID.randomUUID());

		given(invitationRepository.findByIdAndInviteeUserId(invitation.getId(), invitee.getId()))
				.willReturn(Optional.of(invitation));
		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, invitee.getId(), RoomMemberStatus.ACTIVE))
				.willReturn(Optional.empty());
		given(roomMemberRepository.findByRoomIdAndUserId(roomId, invitee.getId())).willReturn(Optional.empty());
		given(userPublicService.getUser(invitee.getId())).willReturn(userResult(invitee));

		InvitationResult result = projectRoomMemberService.acceptInvitation(invitee.getId(), invitation.getId());

		assertThat(result.status()).isEqualTo(InvitationStatus.ACCEPTED);
		assertThat(result.acceptedAt()).isNotNull();

		ArgumentCaptor<RoomMember> memberCaptor = ArgumentCaptor.forClass(RoomMember.class);
		verify(roomMemberRepository).save(memberCaptor.capture());
		assertThat(memberCaptor.getValue().getRoomId()).isEqualTo(roomId);
		assertThat(memberCaptor.getValue().getUserId()).isEqualTo(invitee.getId());
		assertThat(memberCaptor.getValue().getStatus()).isEqualTo(RoomMemberStatus.ACTIVE);
	}

	@Test
	void projectLeaderCanCancelPendingInvitation() {
		UUID roomId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		User invitee = user(UUID.randomUUID(), "invitee", "준화");
		Invitation invitation = Invitation.create(
				roomId,
				leaderId,
				invitee.getId(),
				RoomMemberRole.MEMBER,
				Instant.now().plusSeconds(3600)
		);
		ReflectionTestUtils.setField(invitation, "id", UUID.randomUUID());

		given(invitationRepository.findById(invitation.getId())).willReturn(Optional.of(invitation));
		given(userPublicService.getUser(invitee.getId())).willReturn(userResult(invitee));

		InvitationResult result = projectRoomMemberService.cancelInvitation(leaderId, invitation.getId());

		assertThat(result.status()).isEqualTo(InvitationStatus.CANCELED);
	}

	@Test
	void inviteeCanListPendingReceivedInvitations() {
		UUID roomId = UUID.randomUUID();
		User inviter = user(UUID.randomUUID(), "leader", "미연");
		User invitee = user(UUID.randomUUID(), "invitee", "민서");
		ProjectRoom room = ProjectRoom.create(inviter.getId(), "새 프로젝트룸", null, null, null, null, null, null);
		ReflectionTestUtils.setField(room, "id", roomId);
		Invitation invitation = Invitation.create(
				roomId,
				inviter.getId(),
				invitee.getId(),
				RoomMemberRole.MEMBER,
				Instant.now().plusSeconds(3600)
		);
		ReflectionTestUtils.setField(invitation, "id", UUID.randomUUID());
		PageRequest pageable = PageRequest.of(0, 20);

		given(invitationRepository.findByInviteeUserIdAndStatus(
				invitee.getId(),
				InvitationStatus.PENDING,
				pageable
		)).willReturn(new PageImpl<>(List.of(invitation), pageable, 1));
		given(userPublicService.getUsers(org.mockito.ArgumentMatchers.<org.springframework.data.domain.Page<UUID>>any()))
				.willReturn(Map.of(inviter.getId(), userResult(inviter)));
		given(userPublicService.getUser(invitee.getId())).willReturn(userResult(invitee));
		given(projectRoomRepository.findAllById(any())).willReturn(List.of(room));

		var result = projectRoomMemberService.getReceivedInvitations(
				invitee.getId(),
				InvitationStatus.PENDING,
				pageable
		);

		assertThat(result.getItems()).hasSize(1);
		InvitationResult item = result.getItems().getFirst();
		assertThat(item.id()).isEqualTo(invitation.getId());
		assertThat(item.roomId()).isEqualTo(roomId);
		assertThat(item.roomName()).isEqualTo("새 프로젝트룸");
		assertThat(item.inviterUserId()).isEqualTo(inviter.getId());
		assertThat(item.inviterName()).isEqualTo("미연");
		assertThat(item.inviteeUserId()).isEqualTo(invitee.getId());
		assertThat(item.status()).isEqualTo(InvitationStatus.PENDING);
	}

	@Test
	void projectLeaderCanUpdateMemberRole() {
		UUID roomId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		User memberUser = user(UUID.randomUUID(), "member", "정현");
		RoomMember member = RoomMember.createMember(roomId, memberUser.getId());

		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, memberUser.getId(), RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(member));
		given(userPublicService.getUser(memberUser.getId())).willReturn(userResult(memberUser));

		var result = projectRoomMemberService.updateMemberRole(
				leaderId,
				roomId,
				memberUser.getId(),
				RoomMemberRole.PROJECT_LEADER
		);

		assertThat(result.role()).isEqualTo(RoomMemberRole.PROJECT_LEADER);
		assertThat(member.getRole()).isEqualTo(RoomMemberRole.PROJECT_LEADER);
	}

	@Test
	void cannotDemoteLastProjectLeader() {
		UUID roomId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		RoomMember leader = RoomMember.createLeader(roomId, leaderId);

		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, leaderId, RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(leader));
		given(roomMemberRepository.findByRoomIdAndStatusForUpdate(roomId, RoomMemberStatus.ACTIVE))
				.willReturn(List.of(leader));

		assertThatThrownBy(() -> projectRoomMemberService.updateMemberRole(
				leaderId,
				roomId,
				leaderId,
				RoomMemberRole.MEMBER
		)).isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_409_004));
		assertThat(leader.getRole()).isEqualTo(RoomMemberRole.PROJECT_LEADER);
	}

	@Test
	void canDemoteProjectLeaderWhenAnotherLeaderRemains() {
		UUID roomId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		User targetUser = user(UUID.randomUUID(), "target-leader", "정현");
		RoomMember requester = RoomMember.createLeader(roomId, leaderId);
		RoomMember target = RoomMember.createLeader(roomId, targetUser.getId());

		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, targetUser.getId(), RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(target));
		given(roomMemberRepository.findByRoomIdAndStatusForUpdate(roomId, RoomMemberStatus.ACTIVE))
				.willReturn(List.of(requester, target));
		given(userPublicService.getUser(targetUser.getId())).willReturn(userResult(targetUser));

		var result = projectRoomMemberService.updateMemberRole(
				leaderId,
				roomId,
				targetUser.getId(),
				RoomMemberRole.MEMBER
		);

		assertThat(result.role()).isEqualTo(RoomMemberRole.MEMBER);
		assertThat(target.getRole()).isEqualTo(RoomMemberRole.MEMBER);
	}

	@Test
	void projectLeaderCanRemoveMember() {
		UUID roomId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		RoomMember member = RoomMember.createMember(roomId, memberId);

		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, memberId, RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(member));

		projectRoomMemberService.removeMember(leaderId, roomId, memberId);

		assertThat(member.getStatus()).isEqualTo(RoomMemberStatus.REMOVED);
	}

	@Test
	void activeMemberCanLeaveRoom() {
		UUID roomId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		RoomMember member = RoomMember.createMember(roomId, memberId);

		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, memberId, RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(member));

		projectRoomMemberService.removeMember(memberId, roomId, memberId);

		assertThat(member.getStatus()).isEqualTo(RoomMemberStatus.LEFT);
	}

	@Test
	void lastProjectLeaderCannotLeaveRoom() {
		UUID roomId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		RoomMember leader = RoomMember.createLeader(roomId, leaderId);

		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, leaderId, RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(leader));
		given(roomMemberRepository.findByRoomIdAndStatusForUpdate(roomId, RoomMemberStatus.ACTIVE))
				.willReturn(List.of(leader));

		assertThatThrownBy(() -> projectRoomMemberService.removeMember(leaderId, roomId, leaderId))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_409_004));
		assertThat(leader.getStatus()).isEqualTo(RoomMemberStatus.ACTIVE);
	}

	private User user(UUID userId, String bubliId, String name) {
		User user = User.createGoogleUser(
				"google-sub-" + bubliId,
				bubliId,
				name,
				null,
				"ko",
				"Asia/Seoul"
		);
		ReflectionTestUtils.setField(user, "id", userId);
		return user;
	}

	private UserResult userResult(User user) {
		return UserResult.from(user);
	}
}
