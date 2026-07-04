package com.bubli.project.repository;

import com.bubli.project.entity.Invitation;
import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.type.InvitationStatus;
import com.bubli.project.type.RoomMemberRole;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
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

	@Autowired
	EntityManager entityManager;

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

	@Test
	void expiredPendingInvitationCanBeExpiredBeforeCreatingNewPendingInvitation() {
		User inviter = userRepository.save(User.createGoogleUser(
				"google-sub-inviter-expired",
				"inviter-expired",
				"미연",
				null,
				"ko",
				"Asia/Seoul"
		));
		User invitee = userRepository.save(User.createGoogleUser(
				"google-sub-invitee-expired",
				"invitee-expired",
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
		Invitation expiredPending = Invitation.create(
				room.getId(),
				inviter.getId(),
				invitee.getId(),
				RoomMemberRole.MEMBER,
				Instant.now().minusSeconds(60)
		);
		invitationRepository.saveAndFlush(expiredPending);
		entityManager.clear();

		int expired = invitationRepository.expirePendingByRoomIdAndInviteeUserId(
				room.getId(),
				invitee.getId(),
				InvitationStatus.PENDING,
				InvitationStatus.EXPIRED,
				Instant.now()
		);
		int inserted = invitationRepository.insertPendingIfAbsent(
				UUID.randomUUID(),
				room.getId(),
				inviter.getId(),
				invitee.getId(),
				RoomMemberRole.MEMBER.name(),
				Instant.now().plusSeconds(3600)
		);
		invitationRepository.flush();
		entityManager.clear();

		assertThat(expired).isEqualTo(1);
		assertThat(inserted).isEqualTo(1);
		assertThat(invitationRepository.findAll())
				.extracting(Invitation::getStatus)
				.containsExactlyInAnyOrder(InvitationStatus.EXPIRED, InvitationStatus.PENDING);
	}
}
