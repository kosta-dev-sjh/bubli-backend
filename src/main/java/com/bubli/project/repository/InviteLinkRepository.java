package com.bubli.project.repository;

import com.bubli.project.entity.InviteLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InviteLinkRepository extends JpaRepository<InviteLink, UUID> {

	Optional<InviteLink> findByToken(String token);
}
