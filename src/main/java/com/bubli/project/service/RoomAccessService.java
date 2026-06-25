package com.bubli.project.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.entity.RoomMember;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.RoomMemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomAccessService {

	private final RoomMemberRepository roomMemberRepository;

	@Transactional(readOnly = true)
	public void validateActiveMember(UUID userId, UUID roomId) {
		boolean activeMember = roomMemberRepository.existsByRoomIdAndUserIdAndStatus(
				roomId,
				userId,
				RoomMemberStatus.ACTIVE
		);
		if (!activeMember) {
			throw new BusinessException(ErrorCode.PROJECT_403_001);
		}
	}

	@Transactional(readOnly = true)
	public List<UUID> findActiveRoomIds(UUID userId) {
		return roomMemberRepository.findByUserIdAndStatus(userId, RoomMemberStatus.ACTIVE)
				.stream()
				.map(RoomMember::getRoomId)
				.toList();
	}
}
