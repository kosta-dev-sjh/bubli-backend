package com.bubli.resource.service;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobStatus;
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
import com.bubli.resource.type.DocumentType;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.storage.service.FileStorage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
        FileStorage fileStorage = mock(FileStorage.class);
        DocumentFileInspector inspector = new DocumentFileInspector();
        UUID resourceId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(resourceFileRepository.existsActiveRoomFileByChecksum(any(), anyString())).thenReturn(false);
        when(fileStorage.store(anyString(), any())).thenReturn("resources/room/sample.txt");
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
        when(agentJobRepository.save(any(AgentJob.class))).thenAnswer(invocation -> {
            AgentJob job = invocation.getArgument(0);
            ReflectionTestUtils.setField(job, "id", jobId);
            return job;
        });

        ContractDocumentUploadResponse response = new DocumentUploadService(
                resourceRepository,
                resourceFileRepository,
                resourceVersionRepository,
                agentJobRepository,
                fileStorage,
                inspector
        ).uploadContractDocument(
                UUID.randomUUID(),
                UUID.randomUUID(),
                DocumentType.REQUIREMENT,
                textFile()
        );

        assertThat(response.resourceId()).isEqualTo(resourceId);
        assertThat(response.jobId()).isEqualTo(jobId);
        assertThat(response.status()).isEqualTo(AgentJobStatus.PENDING);
        verify(resourceRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(resource ->
                resource.getStatus() == ResourceStatus.ANALYZING
        ));
        verify(agentJobRepository).save(org.mockito.ArgumentMatchers.argThat(job ->
                job.getJobType() == AgentJobType.ANALYZE_RESOURCE
                        && job.getResourceId().equals(resourceId)
        ));
    }

    @Test
    void rejectsDuplicateBeforeWritingFile() {
        ResourceRepository resourceRepository = mock(ResourceRepository.class);
        ResourceFileRepository resourceFileRepository = mock(ResourceFileRepository.class);
        ResourceVersionRepository resourceVersionRepository = mock(ResourceVersionRepository.class);
        AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
        FileStorage fileStorage = mock(FileStorage.class);

        when(resourceFileRepository.existsActiveRoomFileByChecksum(any(), anyString())).thenReturn(true);

        DocumentUploadService service = new DocumentUploadService(
                resourceRepository,
                resourceFileRepository,
                resourceVersionRepository,
                agentJobRepository,
                fileStorage,
                new DocumentFileInspector()
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

        verify(fileStorage, never()).store(anyString(), any());
        verify(resourceRepository, never()).saveAndFlush(any());
        verify(agentJobRepository, never()).save(any());
    }

    @Test
    void rejectsUnsupportedContractDocumentType() {
        DocumentUploadService service = new DocumentUploadService(
                mock(ResourceRepository.class),
                mock(ResourceFileRepository.class),
                mock(ResourceVersionRepository.class),
                mock(AgentJobRepository.class),
                mock(FileStorage.class),
                new DocumentFileInspector()
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
