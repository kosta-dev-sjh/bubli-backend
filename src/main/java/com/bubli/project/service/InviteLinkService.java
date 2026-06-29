package com.bubli.project.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.dto.InviteLinkResponse;
import com.bubli.project.entity.InviteLink;
import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.entity.RoomMember;
import com.bubli.project.repository.InviteLinkRepository;
import com.bubli.project.repository.ProjectRoomRepository;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.RoomMemberStatus;
import com.bubli.user.dto.UserResult;
import com.bubli.user.service.UserPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InviteLinkService {

	private static final int DEFAULT_EXPIRES_IN_HOURS = 72;

	private final InviteLinkRepository inviteLinkRepository;
	private final ProjectRoomRepository projectRoomRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final UserPublicService userPublicService;
	private final ProjectMembershipPublicService projectMembershipPublicService;

	@Transactional
	public InviteLinkResponse createInviteLink(UUID userId, UUID roomId, int expiresInHours) {
		projectMembershipPublicService.assertProjectLeader(userId, roomId);

		ProjectRoom room = projectRoomRepository.findById(roomId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_404_001));

		int hours = expiresInHours <= 0 ? DEFAULT_EXPIRES_IN_HOURS : expiresInHours;
		InviteLink link = inviteLinkRepository.save(InviteLink.create(roomId, userId, hours));

		UserResult inviter = userPublicService.getUser(userId);
		return toResponse(link, room.getName(), inviter.name());
	}

	@Transactional(readOnly = true)
	public InviteLinkResponse getInviteLink(String token) {
		InviteLink link = inviteLinkRepository.findByToken(token)
				.orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_404_003));

		ProjectRoom room = projectRoomRepository.findById(link.getRoomId())
				.orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_404_001));

		UserResult inviter = userPublicService.getUser(link.getCreatedByUserId());
		return toResponse(link, room.getName(), inviter.name());
	}

	@Transactional
	public InviteLinkResponse acceptInviteLink(UUID userId, String token) {
		InviteLink link = inviteLinkRepository.findByToken(token)
				.orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_404_003));

		if (link.isExpired()) {
			throw new BusinessException(ErrorCode.PROJECT_410_001);
		}

		RoomMember roomMember = roomMemberRepository.findByRoomIdAndUserId(link.getRoomId(), userId)
				.orElseGet(() -> RoomMember.createMember(link.getRoomId(), userId));
		roomMember.reactivate(null);
		roomMemberRepository.save(roomMember);

		ProjectRoom room = projectRoomRepository.findById(link.getRoomId())
				.orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_404_001));

		UserResult inviter = userPublicService.getUser(link.getCreatedByUserId());
		return toResponse(link, room.getName(), inviter.name());
	}

	private InviteLinkResponse toResponse(InviteLink link, String roomName, String inviterName) {
		return new InviteLinkResponse(
				link.getToken(),
				link.getRoomId(),
				roomName,
				inviterName,
				link.getExpiresAt(),
				link.isExpired()
		);
	}
}
