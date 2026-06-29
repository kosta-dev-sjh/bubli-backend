package com.bubli.resource.repository;

import com.bubli.resource.entity.AiDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository("resourceAiDocumentRepository")
public interface AiDocumentRepository extends JpaRepository<AiDocument, UUID> {

    Optional<AiDocument> findByResourceId(UUID resourceId);
}
