package com.bubli.project.service;

import com.bubli.chat.service.RoomChatPublicService;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.personal.notification.service.NotificationPublicService;
import com.bubli.personal.notification.type.NotificationSourceType;
import com.bubli.personal.timer.service.TimeLogPublicService;
import com.bubli.project.dto.CreateInvitationCommand;
import com.bubli.project.dto.InvitationResult;
import com.bubli.project.dto.ProjectRoomMemberResult;
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
import com.bubli.user.service.FriendshipPublicService;
import com.bubli.user.service.UserPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectRoomMemberService {

	private static final int DEFAULT_INVITATION_EXPIRE_DAYS = 7;

	private final RoomMemberRepository roomMemberRepository;
	private final ProjectRoomRepository projectRoomRepository;
	private final InvitationRepository invitationRepository;
	private final UserPublicService userPublicService;
	private final FriendshipPublicService friendshipPublicService;
	private final ProjectMembershipPublicService projectMembershipPublicService;
	private final RoomChatPublicService roomChatPublicService;
	private final TimeLogPublicService timeLogPublicService;
	private final NotificationPublicService notificationPublicService;

	@Transactional(readOnly = true)
	public PageResponse<ProjectRoomMemberResult> getMembers(UUID requesterId, UUID roomId, Pageable pageable) {
		projectMembershipPublicService.assertActiveMember(requesterId, roomId);

		Page<RoomMember> page = roomMemberRepository.findByRoomIdAndStatus(roomId, RoomMemberStatus.ACTIVE, pageable);
		Map<UUID, UserResult> users = userPublicService.getUsers(page.map(RoomMember::getUserId));

		return new PageResponse<>(
				page.getContent().stream()
						.map(member -> ProjectRoomMemberResult.from(member, users.get(member.getUserId())))
						.toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext()
		);
	}

	@Transactional
	public InvitationResult createInvitation(UUID inviterUserId, UUID roomId, CreateInvitationCommand command) {
		projectMembershipPublicService.assertProjectLeader(inviterUserId, roomId);

		UserResult invitee = userPublicService.getUser(command.inviteeUserId());

		roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, invitee.id(), RoomMemberStatus.ACTIVE)
				.ifPresent(member -> {
					throw new BusinessException(ErrorCode.PROJECT_409_001);
				});
		friendshipPublicService.assertAcceptedFriend(inviterUserId, invitee.id());

		Instant now = Instant.now();
		expirePendingInvitationsForPair(roomId, invitee.id(), now);
		if (invitationRepository.existsByRoomIdAndInviteeUserIdAndStatus(
				roomId,
				invitee.id(),
				InvitationStatus.PENDING
		)) {
			throw new BusinessException(ErrorCode.PROJECT_409_003);
		}

		UUID invitationId = UUID.randomUUID();
		RoomMemberRole role = command.role() == null ? RoomMemberRole.MEMBER : command.role();
		Instant expiresAt = command.expiresAt() == null
				? now.plus(DEFAULT_INVITATION_EXPIRE_DAYS, ChronoUnit.DAYS)
				: command.expiresAt();
		if (!expiresAt.isAfter(now)) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}

		int inserted = invitationRepository.insertPendingIfAbsent(
				invitationId,
				roomId,
				inviterUserId,
				invitee.id(),
				role.name(),
				expiresAt
		);
		if (inserted == 0) {
			throw new BusinessException(ErrorCode.PROJECT_409_003);
		}

		Invitation invitation = invitationRepository.findById(invitationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_404_002));

		UserResult inviter = userPublicService.getUser(inviterUserId);
		String roomName = projectRoomRepository.findById(roomId).map(ProjectRoom::getName).orElse("");
		notificationPublicService.create(
				invitee.id(),
				NotificationSourceType.ROOM_INVITE,
				invitation.getId(),
				inviter.name(),
				roomName + " 프로젝트룸에 초대했습니다"
		);

		return InvitationResult.from(invitation, invitee);
	}

	@Transactional
	public PageResponse<InvitationResult> getInvitations(UUID requesterId, UUID roomId, Pageable pageable) {
		projectMembershipPublicService.assertProjectLeader(requesterId, roomId);

		expirePendingInvitationsForRoom(roomId, Instant.now());
		Page<Invitation> page = invitationRepository.findByRoomId(roomId, pageable);
		Map<UUID, UserResult> invitees = userPublicService.getUsers(page.map(Invitation::getInviteeUserId));

		return new PageResponse<>(
				page.getContent().stream()
						.map(invitation -> InvitationResult.from(invitation, invitees.get(invitation.getInviteeUserId())))
						.toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext()
		);
	}

	@Transactional
	public PageResponse<InvitationResult> getReceivedInvitations(
			UUID inviteeUserId,
			InvitationStatus status,
			Pageable pageable
	) {
		expirePendingInvitationsForInvitee(inviteeUserId, Instant.now());
		Page<Invitation> page = invitationRepository.findByInviteeUserIdAndStatus(inviteeUserId, status, pageable);
		Map<UUID, UserResult> inviters = userPublicService.getUsers(page.map(Invitation::getInviterUserId));
		UserResult invitee = userPublicService.getUser(inviteeUserId);
		Map<UUID, ProjectRoom> rooms = projectRoomRepository.findAllById(
						page.getContent().stream()
								.map(Invitation::getRoomId)
								.collect(Collectors.toSet())
				).stream()
				.collect(Collectors.toMap(ProjectRoom::getId, Function.identity()));

		return new PageResponse<>(
				page.getContent().stream()
						.map(invitation -> InvitationResult.from(
								invitation,
								rooms.get(invitation.getRoomId()),
								inviters.get(invitation.getInviterUserId()),
								invitee
						))
						.toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext()
		);
	}

	@Transactional
	public InvitationResult acceptInvitation(UUID userId, UUID invitationId) {
		Invitation invitation = invitationRepository.findByIdAndInviteeUserIdForUpdate(invitationId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_404_002));

		if (!invitation.isPending()) {
			throw new BusinessException(ErrorCode.PROJECT_409_002);
		}

		Instant now = Instant.now();
		if (invitation.isExpired(now)) {
			invitation.expire();
			throw new BusinessException(ErrorCode.PROJECT_409_002);
		}

		roomMemberRepository.findByRoomIdAndUserIdAndStatus(
				invitation.getRoomId(),
				userId,
				RoomMemberStatus.ACTIVE
		).ifPresent(member -> {
			throw new BusinessException(ErrorCode.PROJECT_409_001);
		});

		RoomMember roomMember = roomMemberRepository.findByRoomIdAndUserId(invitation.getRoomId(), userId)
				.orElseGet(() -> RoomMember.create(invitation.getRoomId(), userId, invitation.getRole()));
		roomMember.reactivate(invitation.getRole());
		roomMemberRepository.save(roomMember);

		invitation.accept(now);
		roomChatPublicService.addMember(invitation.getRoomId(), userId);

		UserResult invitee = userPublicService.getUser(userId);
		return InvitationResult.from(invitation, invitee);
	}

	@Transactional
	public InvitationResult cancelInvitation(UUID requesterId, UUID invitationId) {
		Invitation invitation = invitationRepository.findByIdForUpdate(invitationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_404_002));
		projectMembershipPublicService.assertProjectLeader(requesterId, invitation.getRoomId());

		if (!invitation.isPending()) {
			throw new BusinessException(ErrorCode.PROJECT_409_002);
		}

		invitation.cancel();
		UserResult invitee = userPublicService.getUser(invitation.getInviteeUserId());
		return InvitationResult.from(invitation, invitee);
	}

	@Transactional
	public ProjectRoomMemberResult updateMemberRole(UUID requesterId, UUID roomId, UUID memberUserId, RoomMemberRole role) {
		projectMembershipPublicService.assertProjectLeader(requesterId, roomId);

		RoomMember member = roomMemberRepository.findByRoomIdAndUserIdAndStatus(
				roomId,
				memberUserId,
				RoomMemberStatus.ACTIVE
		).orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_403_001));

		RoomMemberRole nextRole = normalizeRole(role);
		if (member.getRole() == RoomMemberRole.PROJECT_LEADER && nextRole != RoomMemberRole.PROJECT_LEADER) {
			assertProjectRoomKeepsLeader(member);
		}

		member.updateRole(nextRole);
		UserResult user = userPublicService.getUser(memberUserId);
		return ProjectRoomMemberResult.from(member, user);
	}

	@Transactional
	public void removeMember(UUID requesterId, UUID roomId, UUID memberUserId) {
		RoomMember member = roomMemberRepository.findByRoomIdAndUserIdAndStatus(
				roomId,
				memberUserId,
				RoomMemberStatus.ACTIVE
		).orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_403_001));

		if (requesterId.equals(memberUserId)) {
			assertProjectRoomKeepsLeader(member);
			timeLogPublicService.stopRunningRoomTimer(memberUserId, roomId);
			member.leave();
			roomChatPublicService.removeMember(roomId, memberUserId);
			return;
		}

		projectMembershipPublicService.assertProjectLeader(requesterId, roomId);
		assertProjectRoomKeepsLeader(member);
		timeLogPublicService.stopRunningRoomTimer(memberUserId, roomId);
		member.remove();
		roomChatPublicService.removeMember(roomId, memberUserId);
	}

	private void expirePendingInvitationsForPair(UUID roomId, UUID inviteeUserId, Instant now) {
		invitationRepository.expirePendingByRoomIdAndInviteeUserId(
				roomId,
				inviteeUserId,
				InvitationStatus.PENDING,
				InvitationStatus.EXPIRED,
				now
		);
	}

	private void expirePendingInvitationsForRoom(UUID roomId, Instant now) {
		invitationRepository.expirePendingByRoomId(
				roomId,
				InvitationStatus.PENDING,
				InvitationStatus.EXPIRED,
				now
		);
	}

	private void expirePendingInvitationsForInvitee(UUID inviteeUserId, Instant now) {
		invitationRepository.expirePendingByInviteeUserId(
				inviteeUserId,
				InvitationStatus.PENDING,
				InvitationStatus.EXPIRED,
				now
		);
	}

	private RoomMemberRole normalizeRole(RoomMemberRole role) {
		return role == null ? RoomMemberRole.MEMBER : role;
	}

	private void assertProjectRoomKeepsLeader(RoomMember member) {
		if (member.getRole() != RoomMemberRole.PROJECT_LEADER) {
			return;
		}

		long activeLeaderCount = roomMemberRepository.findByRoomIdAndStatusForUpdate(
						member.getRoomId(),
						RoomMemberStatus.ACTIVE
				).stream()
				.filter(activeMember -> activeMember.getRole() == RoomMemberRole.PROJECT_LEADER)
				.count();

		if (activeLeaderCount <= 1) {
			throw new BusinessException(ErrorCode.PROJECT_409_004);
		}
	}
}
