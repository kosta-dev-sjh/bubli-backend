package com.bubli.agent.repository;

import com.bubli.agent.entity.GeneratedDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GeneratedDocumentRepository extends JpaRepository<GeneratedDocument, UUID> {

    Optional<GeneratedDocument> findBySuggestionId(UUID suggestionId);

    Page<GeneratedDocument> findByUserIdAndRoomIdIsNull(UUID userId, Pageable pageable);

    Page<GeneratedDocument> findByRoomId(UUID roomId, Pageable pageable);
}
