package com.bubli.resource.service;

import com.bubli.resource.entity.Document;
import com.bubli.resource.entity.DocumentChunk;
import com.bubli.resource.repository.DocumentRepository;
import com.bubli.resource.type.DocumentFileType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentLifecycleService {

    private final DocumentRepository documentRepository;

    @Transactional
    public DocumentVersionChange createNextVersion(
            UUID versionGroupId,
            UUID newResourceId,
            String newFileName,
            DocumentFileType newFileType,
            String newStoragePath,
            String newChecksum
    ) {
        Document current = documentRepository.findLatestVersionForUpdate(versionGroupId)
                .orElseThrow(() -> new IllegalArgumentException("최신 문서 버전을 찾을 수 없습니다."));

        List<UUID> retiredVectorStoreIds = activeVectorStoreIds(current);
        Document next = current.createNextVersion(
                newResourceId,
                newFileName,
                newFileType,
                newStoragePath,
                newChecksum
        );

        documentRepository.saveAndFlush(current);
        Document savedNext = documentRepository.save(next);

        return new DocumentVersionChange(savedNext, retiredVectorStoreIds);
    }

    @Transactional
    public List<UUID> markDeleted(UUID documentId) {
        Document document = documentRepository.findByIdForUpdate(documentId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));

        List<UUID> retiredVectorStoreIds = activeVectorStoreIds(document);
        document.markDeleted();
        documentRepository.saveAndFlush(document);
        return retiredVectorStoreIds;
    }

    private List<UUID> activeVectorStoreIds(Document document) {
        return document.getChunks().stream()
                .filter(DocumentChunk::isActive)
                .map(DocumentChunk::getVectorStoreId)
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
