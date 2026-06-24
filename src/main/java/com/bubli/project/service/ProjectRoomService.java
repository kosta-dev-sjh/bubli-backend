package com.bubli.project.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.project.dto.CreateProjectRoomCommand;
import com.bubli.project.dto.ProjectRoomResult;
import com.bubli.project.dto.UpdateProjectRoomCommand;
import com.bubli.project.dto.UpdateProjectRoomPaymentCommand;
import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.entity.RoomMember;
import com.bubli.project.repository.ProjectRoomRepository;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.ProjectRoomStatus;
import com.bubli.project.type.RoomMemberRole;
import com.bubli.project.type.RoomMemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectRoomService {

	private final ProjectRoomRepository projectRoomRepository;
	private final RoomMemberRepository roomMemberRepository;

	@Transactional(readOnly = true)
	public PageResponse<ProjectRoomResult> getProjectRooms(UUID userId, Pageable pageable) {
		Page<ProjectRoomResult> page = projectRoomRepository
				.findAccessibleRooms(userId, RoomMemberStatus.ACTIVE, pageable)
				.map(ProjectRoomResult::from);
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext()
		);
	}

	@Transactional(readOnly = true)
	public ProjectRoomResult getProjectRoom(UUID userId, UUID roomId) {
		checkActiveMember(userId, roomId);
		return projectRoomRepository.findById(roomId)
				.map(ProjectRoomResult::from)
				.orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_404_001));
	}

	@Transactional
	public ProjectRoomResult create(UUID userId, CreateProjectRoomCommand command) {
		ProjectRoom projectRoom = ProjectRoom.create(
				userId,
				command.name(),
				command.clientName(),
				command.contractAmount(),
				command.paymentStatus(),
				command.paymentDueDate(),
				command.paidAt(),
				command.status() == null ? ProjectRoomStatus.ACTIVE : command.status()
		);
		ProjectRoom savedRoom = projectRoomRepository.save(projectRoom);
		roomMemberRepository.save(RoomMember.createLeader(savedRoom.getId(), userId));
		return ProjectRoomResult.from(savedRoom);
	}

	@Transactional
	public ProjectRoomResult updateProjectRoom(UUID userId, UUID roomId, UpdateProjectRoomCommand command) {
		checkProjectLeader(userId, roomId);
		ProjectRoom projectRoom = getRoomOrThrow(roomId);
		projectRoom.updateBasicInfo(command.name(), command.clientName(), command.status(), Instant.now());
		return ProjectRoomResult.from(projectRoom);
	}

	@Transactional
	public ProjectRoomResult updateProjectRoomPayment(
			UUID userId,
			UUID roomId,
			UpdateProjectRoomPaymentCommand command
	) {
		checkProjectLeader(userId, roomId);
		ProjectRoom projectRoom = getRoomOrThrow(roomId);
		projectRoom.updatePayment(
				command.contractAmount(),
				command.paymentStatus(),
				command.paymentDueDate(),
				command.paidAt()
		);
		return ProjectRoomResult.from(projectRoom);
	}

	@Transactional
	public ProjectRoomResult closeProjectRoom(UUID userId, UUID roomId) {
		checkProjectLeader(userId, roomId);
		ProjectRoom projectRoom = getRoomOrThrow(roomId);
		projectRoom.close(Instant.now());
		return ProjectRoomResult.from(projectRoom);
	}

	private void checkActiveMember(UUID userId, UUID roomId) {
		boolean activeMember = roomMemberRepository.existsByRoomIdAndUserIdAndStatus(
				roomId,
				userId,
				RoomMemberStatus.ACTIVE
		);
		if (!activeMember) {
			throw new BusinessException(ErrorCode.PROJECT_403_001);
		}
	}

	private void checkProjectLeader(UUID userId, UUID roomId) {
		RoomMember member = roomMemberRepository.findByRoomIdAndUserIdAndStatus(
				roomId,
				userId,
				RoomMemberStatus.ACTIVE
		).orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_403_001));

		if (member.getRole() != RoomMemberRole.PROJECT_LEADER) {
			throw new BusinessException(ErrorCode.PROJECT_403_002);
		}
	}

	private ProjectRoom getRoomOrThrow(UUID roomId) {
		return projectRoomRepository.findById(roomId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_404_001));
	}
}
