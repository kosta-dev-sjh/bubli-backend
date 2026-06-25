package com.bubli.resource.repository;

import com.bubli.resource.entity.Document;
import com.bubli.resource.type.DocumentStatus;
import com.bubli.resource.type.DocumentType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Optional<Document> findByIdAndStatusNot(UUID id, DocumentStatus excludedStatus);

    Optional<Document> findByVersionGroupIdAndLatestTrue(UUID versionGroupId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "chunks")
    @Query("""
            select d
            from Document d
            where d.versionGroupId = :versionGroupId
              and d.latest = true
            """)
    Optional<Document> findLatestVersionForUpdate(@Param("versionGroupId") UUID versionGroupId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "chunks")
    @Query("""
            select d
            from Document d
            where d.id = :documentId
            """)
    Optional<Document> findByIdForUpdate(@Param("documentId") UUID documentId);

    List<Document> findAllByProjectRoomIdAndStatusAndLatestTrue(
            UUID projectRoomId,
            DocumentStatus status
    );

    List<Document> findAllByProjectRoomIdAndDocumentTypeInAndStatusAndLatestTrue(
            UUID projectRoomId,
            List<DocumentType> documentTypes,
            DocumentStatus status
    );

    boolean existsByProjectRoomIdAndChecksumAndStatusNot(
            UUID projectRoomId,
            String checksum,
            DocumentStatus excludedStatus
    );
}
