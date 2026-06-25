package com.bubli.resource.repository;

import com.bubli.resource.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findAllByDocumentIdAndActiveTrueOrderByChunkIndex(UUID documentId);

    List<DocumentChunk> findAllByDocumentVersionGroupIdAndDocumentLatestTrueAndActiveTrueOrderByChunkIndex(
            UUID versionGroupId
    );

    List<DocumentChunk> findAllByVectorStoreIdIn(List<UUID> vectorStoreIds);
}
