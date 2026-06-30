package com.bubli.resource.service;

import com.bubli.agent.dto.AgentJobTicket;
import com.bubli.agent.service.AgentJobPublicService;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.resource.dto.ContractDocumentUploadResponse;
import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceFile;
import com.bubli.resource.entity.ResourceVersion;
import com.bubli.resource.repository.ResourceFileRepository;
import com.bubli.resource.repository.ResourceRepository;
import com.bubli.resource.repository.ResourceVersionRepository;
import com.bubli.resource.type.DocumentFileType;
import com.bubli.resource.type.DocumentType;
import com.bubli.storage.service.StoragePublicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentUploadService {

    private final ResourceRepository resourceRepository;
    private final ResourceFileRepository resourceFileRepository;
    private final ResourceVersionRepository resourceVersionRepository;
    private final AgentJobPublicService agentJobService;
    private final StoragePublicService storageService;
    private final DocumentFileInspector fileInspector;
    private final ProjectMembershipPublicService projectMembershipPublicService;

    @Transactional
    public ContractDocumentUploadResponse uploadContractDocument(
            UUID roomId,
            UUID ownerId,
            DocumentType documentType,
            MultipartFile file
    ) {
        return uploadContractDocument(roomId, ownerId, documentType, file, true);
    }

    @Transactional
    public ContractDocumentUploadResponse uploadContractDocument(
            UUID roomId,
            UUID ownerId,
            DocumentType documentType,
            MultipartFile file,
            boolean autoAnalyze
    ) {
        require(roomId, "roomId");
        require(ownerId, "ownerId");
        require(documentType, "documentType");
        requireContractDocumentType(documentType);
        projectMembershipPublicService.assertActiveMember(ownerId, roomId);

        DocumentFileInspector.InspectedDocument inspected = fileInspector.inspect(file);
        rejectDuplicate(roomId, inspected.checksum());

        String storageKey = storageKey(roomId, inspected.fileType().name().toLowerCase());
        String storedStorageKey = store(file, storageKey);
        registerRollbackCleanup(storedStorageKey);

        Resource resource = resourceRepository.saveAndFlush(Resource.roomFile(
                ownerId,
                roomId,
                inspected.fileName()
        ));

        ResourceFile resourceFile = resourceFileRepository.saveAndFlush(ResourceFile.create(
                resource.getId(),
                inspected.fileName(),
                mimeType(inspected.fileType()),
                file.getSize(),
                storedStorageKey,
                inspected.checksum()
        ));

        resourceVersionRepository.save(ResourceVersion.first(
                resource.getId(),
                resourceFile.getId(),
                ownerId
        ));

        AgentJobTicket job = null;
        if (autoAnalyze) {
            resource.startAnalysis();
            job = agentJobService.createAnalyzeResourceJob(
                    ownerId,
                    roomId,
                    resource.getId(),
                    Map.of(
                            "source", "PROJECT_ROOM_DOCUMENT_UPLOAD",
                            "documentTypeHint", documentType.name(),
                            "originalName", inspected.fileName()
                    )
            );
        } else {
            resource.markReady();
        }

        return ContractDocumentUploadResponse.of(resource, job, autoAnalyze);
    }

    private void rejectDuplicate(UUID roomId, String checksum) {
        if (resourceFileRepository.existsActiveRoomFileByChecksum(roomId, checksum)) {
            throw new BusinessException(ErrorCode.RESOURCE_409_001);
        }
    }

    private String store(MultipartFile file, String storageKey) {
        try {
            return storageService.store(storageKey, file.getInputStream());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.RESOURCE_500_001);
        }
    }

    private void registerRollbackCleanup(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        storageService.delete(storageKey);
                    } catch (RuntimeException e) {
                        log.error("Failed to delete uploaded resource file after transaction rollback. storageKey={}",
                                storageKey, e);
                    }
                }
            }
        });
    }

    private String storageKey(UUID roomId, String extension) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return "resources/%s/%d/%02d/%s.%s".formatted(
                roomId,
                today.getYear(),
                today.getMonthValue(),
                UUID.randomUUID(),
                extension
        );
    }

    private void requireContractDocumentType(DocumentType documentType) {
        if (documentType != DocumentType.CONTRACT && documentType != DocumentType.REQUIREMENT) {
            throw new BusinessException(ErrorCode.COMMON_400_002);
        }
    }

    private String mimeType(DocumentFileType fileType) {
        return switch (fileType) {
            case PDF -> "application/pdf";
            case TXT -> "text/plain; charset=utf-8";
        };
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value;
    }
}
