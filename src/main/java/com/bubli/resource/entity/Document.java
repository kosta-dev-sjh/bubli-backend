package com.bubli.resource.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.resource.type.DocumentFileType;
import com.bubli.resource.type.DocumentScope;
import com.bubli.resource.type.DocumentStatus;
import com.bubli.resource.type.DocumentType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "documents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_documents_version_group_version",
                        columnNames = {"version_group_id", "document_version"}
                )
        },
        indexes = {
                @Index(name = "idx_documents_room_status", columnList = "project_room_id,status"),
                @Index(name = "idx_documents_owner_scope", columnList = "owner_id,scope"),
                @Index(name = "idx_documents_checksum", columnList = "checksum"),
                @Index(name = "idx_documents_latest_version", columnList = "version_group_id,is_latest")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "project_room_id")
    private UUID projectRoomId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "version_group_id", nullable = false)
    private UUID versionGroupId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private DocumentFileType fileType;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private DocumentScope scope;

    @Column(name = "storage_path", nullable = false, length = 1000)
    private String storagePath;

    @Column(name = "checksum", nullable = false, length = 64)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DocumentStatus status;

    @Column(name = "document_version", nullable = false)
    private int documentVersion;

    @Column(name = "is_latest", nullable = false)
    private boolean latest;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @OneToMany(
            mappedBy = "document",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private final List<DocumentChunk> chunks = new ArrayList<>();

    private Document(
            UUID resourceId,
            UUID projectRoomId,
            UUID ownerId,
            UUID versionGroupId,
            String fileName,
            DocumentFileType fileType,
            DocumentType documentType,
            DocumentScope scope,
            String storagePath,
            String checksum,
            int documentVersion
    ) {
        this.resourceId = resourceId;
        this.projectRoomId = projectRoomId;
        this.ownerId = require(ownerId, "ownerId");
        this.versionGroupId = require(versionGroupId, "versionGroupId");
        this.fileName = requireText(fileName, "fileName");
        this.fileType = require(fileType, "fileType");
        this.documentType = require(documentType, "documentType");
        this.scope = require(scope, "scope");
        this.storagePath = requireText(storagePath, "storagePath");
        this.checksum = requireText(checksum, "checksum");
        if (documentVersion < 1) {
            throw new IllegalArgumentException("documentVersion은 1 이상이어야 합니다.");
        }
        if (scope == DocumentScope.PROJECT && projectRoomId == null) {
            throw new IllegalArgumentException("PROJECT 문서에는 projectRoomId가 필요합니다.");
        }
        this.documentVersion = documentVersion;
        this.status = DocumentStatus.UPLOADED;
        this.latest = true;
        this.uploadedAt = Instant.now();
    }
    //최초 문서 생성
    public static Document createFirstVersion(
            UUID resourceId,
            UUID projectRoomId,
            UUID ownerId,
            String fileName,
            DocumentFileType fileType,
            DocumentType documentType,
            DocumentScope scope,
            String storagePath,
            String checksum
    ) {
        return new Document(
                resourceId,
                projectRoomId,
                ownerId,
                UUID.randomUUID(),
                fileName,
                fileType,
                documentType,
                scope,
                storagePath,
                checksum,
                1
        );
    }

    public Document createNextVersion(
            UUID newResourceId,
            String newFileName,
            DocumentFileType newFileType,
            String newStoragePath,
            String newChecksum
    ) {
        ensureNotDeleted();
        latest = false;
        deactivateChunks();

        return new Document(
                newResourceId,
                projectRoomId,
                ownerId,
                versionGroupId,
                newFileName,
                newFileType,
                documentType,
                scope,
                newStoragePath,
                newChecksum,
                documentVersion + 1
        );
    }

    public void startExtracting() {
        changeStatus(DocumentStatus.UPLOADED, DocumentStatus.EXTRACTING);
    }

    public void startIndexing() {
        changeStatus(DocumentStatus.EXTRACTING, DocumentStatus.INDEXING);
    }

    public void markReady() {
        changeStatus(DocumentStatus.INDEXING, DocumentStatus.READY);
        processedAt = Instant.now();
        errorMessage = null;
    }

    public void markFailed(String message) {
        ensureNotDeleted();
        status = DocumentStatus.FAILED;
        errorMessage = requireText(message, "errorMessage");
        processedAt = Instant.now();
    }

    public void markDeleted() {
        if (status == DocumentStatus.DELETED) {
            return;
        }
        status = DocumentStatus.DELETED;
        latest = false;
        deletedAt = Instant.now();
        deactivateChunks();
    }

    public DocumentChunk addChunk(
            int chunkIndex,
            String content,
            Integer pageNumber,
            String sectionTitle,
            int tokenCount
    ) {
        ensureNotDeleted();
        DocumentChunk chunk = DocumentChunk.create(
                this,
                chunkIndex,
                content,
                pageNumber,
                sectionTitle,
                tokenCount
        );
        chunks.add(chunk);
        return chunk;
    }

    public List<DocumentChunk> getChunks() {
        return Collections.unmodifiableList(chunks);
    }

    private void deactivateChunks() {
        chunks.forEach(DocumentChunk::deactivate);
    }

    private void changeStatus(DocumentStatus expected, DocumentStatus next) {
        if (status != expected) {
            throw new IllegalStateException(
                    "문서 상태를 %s에서 %s로 변경할 수 없습니다. 현재 상태: %s"
                            .formatted(expected, next, status)
            );
        }
        status = next;
    }

    private void ensureNotDeleted() {
        if (status == DocumentStatus.DELETED) {
            throw new IllegalStateException("삭제된 문서는 변경할 수 없습니다.");
        }
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + "는 필수입니다.");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "는 필수입니다.");
        }
        return value;
    }
}
