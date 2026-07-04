package com.bubli.project.repository;

import com.bubli.project.entity.Invitation;
import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.type.InvitationStatus;
import com.bubli.project.type.RoomMemberRole;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@Transactional
class InvitationRepositoryIntegrationTest extends PostgresIntegrationTestSupport {

	@Autowired
	UserRepository userRepository;

	@Autowired
	ProjectRoomRepository projectRoomRepository;

	@Autowired
	InvitationRepository invitationRepository;

	@Test
	void insertPendingIfAbsentKeepsOnlyOnePendingInvitationPerRoomAndInvitee() {
		User inviter = userRepository.save(User.createGoogleUser(
				"google-sub-inviter-pending",
				"inviter-pending",
				"미연",
				null,
				"ko",
				"Asia/Seoul"
		));
		User invitee = userRepository.save(User.createGoogleUser(
				"google-sub-invitee-pending",
				"invitee-pending",
				"민서",
				null,
				"ko",
				"Asia/Seoul"
		));
		ProjectRoom room = projectRoomRepository.save(ProjectRoom.create(
				inviter.getId(),
				"앱 리뉴얼",
				null,
				null,
				null,
				null,
				null,
				null
		));
		Instant expiresAt = Instant.parse("2026-07-12T09:00:00Z");

		int first = invitationRepository.insertPendingIfAbsent(
				UUID.randomUUID(),
				room.getId(),
				inviter.getId(),
				invitee.getId(),
				RoomMemberRole.MEMBER.name(),
				expiresAt
		);
		int duplicate = invitationRepository.insertPendingIfAbsent(
				UUID.randomUUID(),
				room.getId(),
				inviter.getId(),
				invitee.getId(),
				RoomMemberRole.PROJECT_LEADER.name(),
				expiresAt
		);

		assertThat(first).isEqualTo(1);
		assertThat(duplicate).isZero();
		assertThat(invitationRepository.findAll()).hasSize(1);
		Invitation invitation = invitationRepository
				.findByRoomIdAndInviteeUserIdAndStatus(room.getId(), invitee.getId(), InvitationStatus.PENDING)
				.orElseThrow();
		assertThat(invitation.getRole()).isEqualTo(RoomMemberRole.MEMBER);
		assertThat(invitation.getInviterUserId()).isEqualTo(inviter.getId());
	}
}
