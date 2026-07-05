package com.bubli.project.repository;

import com.bubli.project.entity.Invitation;
import com.bubli.project.type.InvitationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

	Page<Invitation> findByRoomId(UUID roomId, Pageable pageable);

	Page<Invitation> findByInviteeUserIdAndStatus(UUID inviteeUserId, InvitationStatus status, Pageable pageable);

	boolean existsByRoomIdAndInviteeUserIdAndStatus(UUID roomId, UUID inviteeUserId, InvitationStatus status);

	Optional<Invitation> findByRoomIdAndInviteeUserIdAndStatus(UUID roomId, UUID inviteeUserId, InvitationStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select invitation
			from Invitation invitation
			where invitation.id = :id
			""")
	Optional<Invitation> findByIdForUpdate(@Param("id") UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select invitation
			from Invitation invitation
			where invitation.id = :id
			  and invitation.inviteeUserId = :inviteeUserId
			""")
	Optional<Invitation> findByIdAndInviteeUserIdForUpdate(
			@Param("id") UUID id,
			@Param("inviteeUserId") UUID inviteeUserId
	);

	@Modifying
	@Query("""
			update Invitation invitation
			set invitation.status = :expiredStatus,
			    invitation.updatedAt = :now
			where invitation.roomId = :roomId
			  and invitation.inviteeUserId = :inviteeUserId
			  and invitation.status = :pendingStatus
			  and invitation.expiresAt <= :now
			""")
	int expirePendingByRoomIdAndInviteeUserId(
			@Param("roomId") UUID roomId,
			@Param("inviteeUserId") UUID inviteeUserId,
			@Param("pendingStatus") InvitationStatus pendingStatus,
			@Param("expiredStatus") InvitationStatus expiredStatus,
			@Param("now") Instant now
	);

	@Modifying
	@Query("""
			update Invitation invitation
			set invitation.status = :expiredStatus,
			    invitation.updatedAt = :now
			where invitation.roomId = :roomId
			  and invitation.status = :pendingStatus
			  and invitation.expiresAt <= :now
			""")
	int expirePendingByRoomId(
			@Param("roomId") UUID roomId,
			@Param("pendingStatus") InvitationStatus pendingStatus,
			@Param("expiredStatus") InvitationStatus expiredStatus,
			@Param("now") Instant now
	);

	@Modifying
	@Query("""
			update Invitation invitation
			set invitation.status = :expiredStatus,
			    invitation.updatedAt = :now
			where invitation.inviteeUserId = :inviteeUserId
			  and invitation.status = :pendingStatus
			  and invitation.expiresAt <= :now
			""")
	int expirePendingByInviteeUserId(
			@Param("inviteeUserId") UUID inviteeUserId,
			@Param("pendingStatus") InvitationStatus pendingStatus,
			@Param("expiredStatus") InvitationStatus expiredStatus,
			@Param("now") Instant now
	);

	@Modifying
	@Query(value = """
		insert into invitations (
			id,
			room_id,
			inviter_user_id,
			invitee_user_id,
			role,
			status,
			expires_at,
			accepted_at,
			created_at,
			updated_at
		)
		values (
			:id,
			:roomId,
			:inviterUserId,
			:inviteeUserId,
			:role,
			'PENDING',
			:expiresAt,
			null,
			now(),
			now()
		)
		on conflict (room_id, invitee_user_id) where status = 'PENDING' do nothing
		""", nativeQuery = true)
	int insertPendingIfAbsent(
			@Param("id") UUID id,
			@Param("roomId") UUID roomId,
			@Param("inviterUserId") UUID inviterUserId,
			@Param("inviteeUserId") UUID inviteeUserId,
			@Param("role") String role,
			@Param("expiresAt") Instant expiresAt
	);
}
