package com.bubli.resource.service;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobType;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.resource.dto.ContractDocumentUploadResponse;
import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceFile;
import com.bubli.resource.entity.ResourceVersion;
import com.bubli.resource.repository.ResourceFileRepository;
import com.bubli.resource.repository.ResourceRepository;
import com.bubli.resource.repository.ResourceVersionRepository;
import com.bubli.resource.type.DocumentFileType;
import com.bubli.resource.type.DocumentType;
import com.bubli.storage.service.FileStorage;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentUploadService {

    private final ResourceRepository resourceRepository;
    private final ResourceFileRepository resourceFileRepository;
    private final ResourceVersionRepository resourceVersionRepository;
    private final AgentJobRepository agentJobRepository;
    private final FileStorage fileStorage;
    private final DocumentFileInspector fileInspector;

    @Transactional
    public ContractDocumentUploadResponse uploadContractDocument(
            UUID projectRoomId,
            UUID ownerId,
            DocumentType documentType,
            MultipartFile file
    ) {
        //필수 항목
        require(projectRoomId, "projectRoomId");
        require(ownerId, "ownerId");
        require(documentType, "documentType");
        requireContractDocumentType(documentType);  //계약서/요구사항과같은 내용이 아닌지 확인

        DocumentFileInspector.InspectedDocument inspected = fileInspector.inspect(file);
        rejectDuplicate(projectRoomId, inspected.checksum());

        String storageKey = storageKey(projectRoomId, inspected.fileType().name().toLowerCase());
        String storagePath = store(file, storageKey);
        registerRollbackCleanup(storagePath);

        Resource resource = resourceRepository.saveAndFlush(Resource.roomFile(
                ownerId,
                projectRoomId,
                inspected.fileName()
        ));

        ResourceFile resourceFile = resourceFileRepository.saveAndFlush(ResourceFile.create(
                resource.getId(),
                inspected.fileName(),
                mimeType(inspected.fileType()),
                file.getSize(),
                storagePath,
                inspected.checksum()
        ));

        resourceVersionRepository.save(ResourceVersion.first(
                resource.getId(),
                resourceFile.getId(),
                ownerId
        ));

        resource.startAnalysis();

        AgentJob job = agentJobRepository.save(AgentJob.pending(
                ownerId,
                projectRoomId,
                resource.getId(),
                AgentJobType.ANALYZE_RESOURCE
        ));

        return ContractDocumentUploadResponse.of(resource, job);
    }

    private void rejectDuplicate(UUID projectRoomId, String checksum) {
        if (resourceFileRepository.existsActiveRoomFileByChecksum(projectRoomId, checksum)) {
            throw new BusinessException(ErrorCode.RESOURCE_409_001);
        }
    }

    private String store(MultipartFile file, String storageKey) {
        try {
            return fileStorage.store(storageKey, file.getInputStream());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.RESOURCE_500_001);
        }
    }

    private void registerRollbackCleanup(String storagePath) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        fileStorage.delete(storagePath);
                    } catch (RuntimeException e) {
                        log.error("Failed to delete uploaded resource file after transaction rollback. storagePath={}",
                                storagePath, e);
                    }
                }
            }
        });
    }

    private String storageKey(UUID projectRoomId, String extension) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return "resources/%s/%d/%02d/%s.%s".formatted(
                projectRoomId,
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
