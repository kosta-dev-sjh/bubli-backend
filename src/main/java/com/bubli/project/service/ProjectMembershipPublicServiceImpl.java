package com.bubli.project.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.entity.RoomMember;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.RoomMemberRole;
import com.bubli.project.type.RoomMemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectMembershipPublicServiceImpl implements ProjectMembershipPublicService {

	private final RoomMemberRepository roomMemberRepository;

	@Override
	@Transactional(readOnly = true)
	public void assertActiveMember(UUID userId, UUID roomId) {
		if (!isActiveMember(userId, roomId)) {
			throw new BusinessException(ErrorCode.PROJECT_403_001);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isActiveMember(UUID userId, UUID roomId) {
		return roomMemberRepository.existsByRoomIdAndUserIdAndStatus(
				roomId,
				userId,
				RoomMemberStatus.ACTIVE
		);
	}

	@Override
	@Transactional(readOnly = true)
	public List<UUID> findActiveRoomIds(UUID userId) {
		return roomMemberRepository.findByUserIdAndStatus(userId, RoomMemberStatus.ACTIVE)
				.stream()
				.map(RoomMember::getRoomId)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public void assertProjectLeader(UUID userId, UUID roomId) {
		RoomMember member = roomMemberRepository.findByRoomIdAndUserIdAndStatus(
				roomId,
				userId,
				RoomMemberStatus.ACTIVE
		).orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_403_001));

		if (member.getRole() != RoomMemberRole.PROJECT_LEADER) {
			throw new BusinessException(ErrorCode.PROJECT_403_002);
		}
	}
}
