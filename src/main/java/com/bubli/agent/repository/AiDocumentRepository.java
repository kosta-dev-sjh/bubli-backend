package com.bubli.agent.repository;

import com.bubli.agent.entity.AiDocument;
import com.bubli.agent.type.AiDocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository("agentAiDocumentRepository")
public interface AiDocumentRepository extends JpaRepository<AiDocument, UUID> {

	Optional<AiDocument> findByResourceId(UUID resourceId);

	boolean existsByResourceId(UUID resourceId);

	Page<AiDocument> findByRoomId(UUID roomId, Pageable pageable);

	Page<AiDocument> findByRoomIdAndStatus(UUID roomId, AiDocumentStatus status, Pageable pageable);
}
