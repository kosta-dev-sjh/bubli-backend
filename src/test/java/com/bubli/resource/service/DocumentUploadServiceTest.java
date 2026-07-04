package com.bubli.resource.service;

import com.bubli.agent.dto.AgentJobTicket;
import com.bubli.agent.service.AgentJobPublicService;
import com.bubli.agent.type.AgentJobStatus;
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
import com.bubli.resource.type.DocumentType;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.storage.service.StoragePublicService;
import com.bubli.storage.service.StorageUsagePublicService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentUploadServiceTest {

    @Test
    void storesResourceAndCreatesAnalyzeResourceJob() {
        ResourceRepository resourceRepository = mock(ResourceRepository.class);
        ResourceFileRepository resourceFileRepository = mock(ResourceFileRepository.class);
        ResourceVersionRepository resourceVersionRepository = mock(ResourceVersionRepository.class);
        AgentJobPublicService agentJobService = mock(AgentJobPublicService.class);
        StoragePublicService storageService = mock(StoragePublicService.class);
        StorageUsagePublicService storageUsagePublicService = mock(StorageUsagePublicService.class);
        ProjectMembershipPublicService membershipService = mock(ProjectMembershipPublicService.class);
        DocumentFileInspector inspector = new DocumentFileInspector();
        UUID roomId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(resourceFileRepository.existsActiveRoomFileByChecksum(any(), anyString())).thenReturn(false);
        when(storageService.store(anyString(), any())).thenReturn("resources/room/sample.txt");
        when(resourceRepository.saveAndFlush(any(Resource.class))).thenAnswer(invocation -> {
            Resource resource = invocation.getArgument(0);
            ReflectionTestUtils.setField(resource, "id", resourceId);
            return resource;
        });
        when(resourceFileRepository.saveAndFlush(any(ResourceFile.class))).thenAnswer(invocation -> {
            ResourceFile resourceFile = invocation.getArgument(0);
            ReflectionTestUtils.setField(resourceFile, "id", fileId);
            return resourceFile;
        });
        when(resourceVersionRepository.save(any(ResourceVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agentJobService.createAnalyzeResourceJob(any(), any(), any(), any()))
                .thenReturn(new AgentJobTicket(jobId, AgentJobStatus.PENDING));

        ContractDocumentUploadResponse response = new DocumentUploadService(
                resourceRepository,
                resourceFileRepository,
                resourceVersionRepository,
                agentJobService,
                storageService,
                storageUsagePublicService,
                inspector,
                membershipService
        ).uploadContractDocument(
                roomId,
                UUID.randomUUID(),
                DocumentType.REQUIREMENT,
                textFile()
        );

        assertThat(response.resourceId()).isEqualTo(resourceId);
        assertThat(response.jobId()).isEqualTo(jobId);
        assertThat(response.status()).isEqualTo(AgentJobStatus.PENDING);
        assertThat(response.autoAnalyze()).isTrue();
        verify(resourceRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(resource ->
                resource.getStatus() == ResourceStatus.ANALYZING
        ));
        verify(storageUsagePublicService).recordRoomUpload(roomId, textFile().getSize());
        verify(agentJobService).createAnalyzeResourceJob(any(), any(), eq(resourceId), any());
    }

    @Test
    void rejectsDuplicateBeforeWritingFile() {
        ResourceRepository resourceRepository = mock(ResourceRepository.class);
        ResourceFileRepository resourceFileRepository = mock(ResourceFileRepository.class);
        ResourceVersionRepository resourceVersionRepository = mock(ResourceVersionRepository.class);
        AgentJobPublicService agentJobService = mock(AgentJobPublicService.class);
        StoragePublicService storageService = mock(StoragePublicService.class);
        StorageUsagePublicService storageUsagePublicService = mock(StorageUsagePublicService.class);

        when(resourceFileRepository.existsActiveRoomFileByChecksum(any(), anyString())).thenReturn(true);

        DocumentUploadService service = new DocumentUploadService(
                resourceRepository,
                resourceFileRepository,
                resourceVersionRepository,
                agentJobService,
                storageService,
                storageUsagePublicService,
                new DocumentFileInspector(),
                mock(ProjectMembershipPublicService.class)
        );

        assertThatThrownBy(() -> service.uploadContractDocument(
                UUID.randomUUID(),
                UUID.randomUUID(),
                DocumentType.REQUIREMENT,
                textFile()
        )).isInstanceOfSatisfying(
                BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_409_001)
        );

        verify(storageService, never()).store(anyString(), any());
        verify(storageUsagePublicService, never()).recordRoomUpload(any(), org.mockito.ArgumentMatchers.anyLong());
        verify(resourceRepository, never()).saveAndFlush(any());
        verify(agentJobService, never()).createAnalyzeResourceJob(any(), any(), any(), any());
    }

    @Test
    void rejectsWhenRoomStorageQuotaWouldBeExceededBeforeWritingFile() {
        ResourceFileRepository resourceFileRepository = mock(ResourceFileRepository.class);
        StoragePublicService storageService = mock(StoragePublicService.class);
        StorageUsagePublicService storageUsagePublicService = mock(StorageUsagePublicService.class);
        UUID roomId = UUID.randomUUID();
        when(resourceFileRepository.existsActiveRoomFileByChecksum(any(), anyString())).thenReturn(false);
        BusinessException quotaExceeded = new BusinessException(ErrorCode.STORAGE_400_002);
        org.mockito.Mockito.doThrow(quotaExceeded)
                .when(storageUsagePublicService)
                .recordRoomUpload(eq(roomId), org.mockito.ArgumentMatchers.anyLong());

        DocumentUploadService service = new DocumentUploadService(
                mock(ResourceRepository.class),
                resourceFileRepository,
                mock(ResourceVersionRepository.class),
                mock(AgentJobPublicService.class),
                storageService,
                storageUsagePublicService,
                new DocumentFileInspector(),
                mock(ProjectMembershipPublicService.class)
        );

        assertThatThrownBy(() -> service.uploadContractDocument(
                roomId,
                UUID.randomUUID(),
                DocumentType.REQUIREMENT,
                textFile()
        )).isSameAs(quotaExceeded);

        verify(storageService, never()).store(anyString(), any());
    }

    @Test
    void rejectsUnsupportedContractDocumentType() {
        DocumentUploadService service = new DocumentUploadService(
                mock(ResourceRepository.class),
                mock(ResourceFileRepository.class),
                mock(ResourceVersionRepository.class),
                mock(AgentJobPublicService.class),
                mock(StoragePublicService.class),
                mock(StorageUsagePublicService.class),
                new DocumentFileInspector(),
                mock(ProjectMembershipPublicService.class)
        );

        assertThatThrownBy(() -> service.uploadContractDocument(
                UUID.randomUUID(),
                UUID.randomUUID(),
                DocumentType.GENERAL,
                textFile()
        )).isInstanceOfSatisfying(
                BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.COMMON_400_002)
        );
    }

    private MockMultipartFile textFile() {
        return new MockMultipartFile(
                "file",
                "requirements.txt",
                "text/plain",
                "프로젝트 요구사항".getBytes(StandardCharsets.UTF_8)
        );
    }
}
