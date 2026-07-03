package com.bubli.resource.repository;

import com.bubli.resource.entity.ResourceExtractedText;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResourceExtractedTextRepository extends JpaRepository<ResourceExtractedText, UUID> {

    Optional<ResourceExtractedText> findFirstByResourceIdOrderByUpdatedAtDescIdDesc(UUID resourceId);

    Optional<ResourceExtractedText> findFirstByResourceIdAndLocalFileIdAndExtractionMethodOrderByUpdatedAtDescIdDesc(
            UUID resourceId,
            String localFileId,
            String extractionMethod
    );

    Optional<ResourceExtractedText> findFirstByResourceIdAndChecksumAndExtractionMethodOrderByUpdatedAtDescIdDesc(
            UUID resourceId,
            String checksum,
            String extractionMethod
    );
}
