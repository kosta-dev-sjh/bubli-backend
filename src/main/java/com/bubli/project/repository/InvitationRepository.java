package com.bubli.project.repository;

import com.bubli.project.entity.Invitation;
import com.bubli.project.type.InvitationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

	Page<Invitation> findByRoomId(UUID roomId, Pageable pageable);

	boolean existsByRoomIdAndInviteeUserIdAndStatus(UUID roomId, UUID inviteeUserId, InvitationStatus status);

	Optional<Invitation> findByIdAndInviteeUserId(UUID id, UUID inviteeUserId);
}
