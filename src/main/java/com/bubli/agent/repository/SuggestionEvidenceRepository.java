package com.bubli.agent.repository;

import com.bubli.agent.entity.SuggestionEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SuggestionEvidenceRepository extends JpaRepository<SuggestionEvidence, UUID> {

    List<SuggestionEvidence> findAllBySuggestionIdOrderByCreatedAtAsc(UUID suggestionId);

    List<SuggestionEvidence> findAllByDocumentId(UUID documentId);
}
