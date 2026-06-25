package com.bubli.project.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.dto.CreateInvitationCommand;
import com.bubli.project.dto.InvitationResult;
import com.bubli.project.entity.Invitation;
import com.bubli.project.entity.RoomMember;
import com.bubli.project.repository.InvitationRepository;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.InvitationStatus;
import com.bubli.project.type.RoomMemberRole;
import com.bubli.project.type.RoomMemberStatus;
import com.bubli.user.dto.UserResult;
import com.bubli.user.service.UserPublicService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectRoomMemberServiceTest {

	@Mock
	RoomMemberRepository roomMemberRepository;

	@Mock
	InvitationRepository invitationRepository;

	@Mock
	UserPublicService userPublicService;

	@InjectMocks
	ProjectRoomMemberService projectRoomMemberService;

	@Test
	void projectLeaderCanCreateInvitation() {
		UUID roomId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		UserResult invitee = user(UUID.randomUUID(), "invitee", "준화");
		RoomMember leader = RoomMember.createLeader(roomId, leaderId);
		CreateInvitationCommand command = new CreateInvitationCommand(invitee.id(), RoomMemberRole.MEMBER, null);

		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, leaderId, RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(leader));
		given(userPublicService.getUser(invitee.id())).willReturn(invitee);
		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, invitee.id(), RoomMemberStatus.ACTIVE))
				.willReturn(Optional.empty());
		given(invitationRepository.existsByRoomIdAndInviteeUserIdAndStatus(
				roomId,
				invitee.id(),
				InvitationStatus.PENDING
		)).willReturn(false);
		given(invitationRepository.save(any(Invitation.class))).willAnswer(invocation -> {
			Invitation invitation = invocation.getArgument(0);
			ReflectionTestUtils.setField(invitation, "id", UUID.randomUUID());
			return invitation;
		});

		InvitationResult result = projectRoomMemberService.createInvitation(leaderId, roomId, command);

		assertThat(result.roomId()).isEqualTo(roomId);
		assertThat(result.inviteeUserId()).isEqualTo(invitee.id());
		assertThat(result.inviteeName()).isEqualTo("준화");
		assertThat(result.role()).isEqualTo(RoomMemberRole.MEMBER);
		assertThat(result.status()).isEqualTo(InvitationStatus.PENDING);
	}

	@Test
	void ordinaryMemberCannotCreateInvitation() {
		UUID roomId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		UserResult invitee = user(UUID.randomUUID(), "invitee", "준화");
		RoomMember member = RoomMember.createMember(roomId, memberId);
		CreateInvitationCommand command = new CreateInvitationCommand(invitee.id(), RoomMemberRole.MEMBER, null);

		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, memberId, RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(member));

		assertThatThrownBy(() -> projectRoomMemberService.createInvitation(memberId, roomId, command))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_403_002));
	}

	@Test
	void cannotCreateInvitationForAlreadyActiveMember() {
		UUID roomId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		UserResult invitee = user(UUID.randomUUID(), "invitee", "준화");
		RoomMember leader = RoomMember.createLeader(roomId, leaderId);
		RoomMember activeMember = RoomMember.createMember(roomId, invitee.id());
		CreateInvitationCommand command = new CreateInvitationCommand(invitee.id(), RoomMemberRole.MEMBER, null);

		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, leaderId, RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(leader));
		given(userPublicService.getUser(invitee.id())).willReturn(invitee);
		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, invitee.id(), RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(activeMember));

		assertThatThrownBy(() -> projectRoomMemberService.createInvitation(leaderId, roomId, command))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_409_001));
	}

	@Test
	void inviteeCanAcceptInvitationAndBecomeRoomMember() {
		UUID roomId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		UserResult invitee = user(UUID.randomUUID(), "invitee", "민서");
		Invitation invitation = Invitation.create(
				roomId,
				leaderId,
				invitee.id(),
				RoomMemberRole.MEMBER,
				Instant.now().plusSeconds(3600)
		);
		ReflectionTestUtils.setField(invitation, "id", UUID.randomUUID());

		given(invitationRepository.findByIdAndInviteeUserId(invitation.getId(), invitee.id()))
				.willReturn(Optional.of(invitation));
		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, invitee.id(), RoomMemberStatus.ACTIVE))
				.willReturn(Optional.empty());
		given(roomMemberRepository.findByRoomIdAndUserId(roomId, invitee.id())).willReturn(Optional.empty());
		given(userPublicService.getUser(invitee.id())).willReturn(invitee);

		InvitationResult result = projectRoomMemberService.acceptInvitation(invitee.id(), invitation.getId());

		assertThat(result.status()).isEqualTo(InvitationStatus.ACCEPTED);
		assertThat(result.acceptedAt()).isNotNull();

		ArgumentCaptor<RoomMember> memberCaptor = ArgumentCaptor.forClass(RoomMember.class);
		verify(roomMemberRepository).save(memberCaptor.capture());
		assertThat(memberCaptor.getValue().getRoomId()).isEqualTo(roomId);
		assertThat(memberCaptor.getValue().getUserId()).isEqualTo(invitee.id());
		assertThat(memberCaptor.getValue().getStatus()).isEqualTo(RoomMemberStatus.ACTIVE);
	}

	@Test
	void projectLeaderCanCancelPendingInvitation() {
		UUID roomId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		UserResult invitee = user(UUID.randomUUID(), "invitee", "준화");
		RoomMember leader = RoomMember.createLeader(roomId, leaderId);
		Invitation invitation = Invitation.create(
				roomId,
				leaderId,
				invitee.id(),
				RoomMemberRole.MEMBER,
				Instant.now().plusSeconds(3600)
		);
		ReflectionTestUtils.setField(invitation, "id", UUID.randomUUID());

		given(invitationRepository.findById(invitation.getId())).willReturn(Optional.of(invitation));
		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, leaderId, RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(leader));
		given(userPublicService.getUser(invitee.id())).willReturn(invitee);

		InvitationResult result = projectRoomMemberService.cancelInvitation(leaderId, invitation.getId());

		assertThat(result.status()).isEqualTo(InvitationStatus.CANCELED);
	}

	@Test
	void projectLeaderCanUpdateMemberRole() {
		UUID roomId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		UserResult memberUser = user(UUID.randomUUID(), "member", "정현");
		RoomMember leader = RoomMember.createLeader(roomId, leaderId);
		RoomMember member = RoomMember.createMember(roomId, memberUser.id());

		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, leaderId, RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(leader));
		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, memberUser.id(), RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(member));
		given(userPublicService.getUser(memberUser.id())).willReturn(memberUser);

		var result = projectRoomMemberService.updateMemberRole(
				leaderId,
				roomId,
				memberUser.id(),
				RoomMemberRole.PROJECT_LEADER
		);

		assertThat(result.role()).isEqualTo(RoomMemberRole.PROJECT_LEADER);
		assertThat(member.getRole()).isEqualTo(RoomMemberRole.PROJECT_LEADER);
	}

	@Test
	void projectLeaderCanRemoveMember() {
		UUID roomId = UUID.randomUUID();
		UUID leaderId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		RoomMember leader = RoomMember.createLeader(roomId, leaderId);
		RoomMember member = RoomMember.createMember(roomId, memberId);

		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, memberId, RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(member));
		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, leaderId, RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(leader));

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

	private UserResult user(UUID userId, String bubliId, String name) {
		return new UserResult(
				userId,
				null,
				bubliId,
				name,
				null,
				"ko",
				"Asia/Seoul"
		);
	}
}
