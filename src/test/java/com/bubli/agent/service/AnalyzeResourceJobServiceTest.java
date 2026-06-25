package com.bubli.agent.service;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
import com.bubli.resource.entity.AiDocument;
import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceFile;
import com.bubli.resource.entity.ResourceSummary;
import com.bubli.resource.repository.AiDocumentRepository;
import com.bubli.resource.repository.ResourceFileRepository;
import com.bubli.resource.repository.ResourceRepository;
import com.bubli.resource.repository.ResourceSummaryRepository;
import com.bubli.resource.type.DocumentType;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.storage.service.FileStorage;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyzeResourceJobServiceTest {

    @Test
    void processesTextResourceAndCreatesAnalysisRows() {
        Fixtures fixtures = fixtures("요구사항: 로그인 기능이 필요합니다.");

        AgentJob result = service(fixtures).process(fixtures.jobId);

        assertThat(result.getStatus()).isEqualTo(AgentJobStatus.SUCCEEDED);
        assertThat(fixtures.resource.getStatus()).isEqualTo(ResourceStatus.ANALYZED);
        verify(fixtures.resourceSummaryRepository).save(any(ResourceSummary.class));
        verify(fixtures.aiDocumentRepository).save(org.mockito.ArgumentMatchers.argThat(aiDocument ->
                aiDocument.getDocumentType() == DocumentType.REQUIREMENT
        ));
    }

    @Test
    void marksJobAndResourceFailedWhenExtractedTextIsBlank() {
        Fixtures fixtures = fixtures("   ");

        AgentJob result = service(fixtures).process(fixtures.jobId);

        assertThat(result.getStatus()).isEqualTo(AgentJobStatus.FAILED);
        assertThat(result.getErrorCode()).isEqualTo("RESOURCE_ANALYSIS_FAILED");
        assertThat(fixtures.resource.getStatus()).isEqualTo(ResourceStatus.FAILED);
    }

    private AnalyzeResourceJobService service(Fixtures fixtures) {
        return new AnalyzeResourceJobService(
                fixtures.agentJobRepository,
                fixtures.resourceRepository,
                fixtures.resourceFileRepository,
                fixtures.resourceSummaryRepository,
                fixtures.aiDocumentRepository,
                fixtures.fileStorage
        );
    }

    private Fixtures fixtures(String fileContent) {
        UUID jobId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        AgentJob job = AgentJob.pending(userId, roomId, resourceId, AgentJobType.ANALYZE_RESOURCE);
        ReflectionTestUtils.setField(job, "id", jobId);

        Resource resource = Resource.roomFile(userId, roomId, "requirements.txt");
        ReflectionTestUtils.setField(resource, "id", resourceId);

        ResourceFile resourceFile = ResourceFile.create(
                resourceId,
                "requirements.txt",
                "text/plain; charset=utf-8",
                fileContent.getBytes(StandardCharsets.UTF_8).length,
                "resources/room/requirements.txt",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        );
        ReflectionTestUtils.setField(resourceFile, "id", UUID.randomUUID());

        AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
        ResourceRepository resourceRepository = mock(ResourceRepository.class);
        ResourceFileRepository resourceFileRepository = mock(ResourceFileRepository.class);
        ResourceSummaryRepository resourceSummaryRepository = mock(ResourceSummaryRepository.class);
        AiDocumentRepository aiDocumentRepository = mock(AiDocumentRepository.class);
        FileStorage fileStorage = mock(FileStorage.class);

        when(agentJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(resourceFileRepository.findTopByResourceIdOrderByCreatedAtDesc(resourceId))
                .thenReturn(Optional.of(resourceFile));
        when(aiDocumentRepository.findByResourceId(resourceId)).thenReturn(Optional.empty());
        when(aiDocumentRepository.save(any(AiDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(resourceSummaryRepository.save(any(ResourceSummary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileStorage.open("resources/room/requirements.txt"))
                .thenReturn(new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8)));

        return new Fixtures(
                jobId,
                resource,
                agentJobRepository,
                resourceRepository,
                resourceFileRepository,
                resourceSummaryRepository,
                aiDocumentRepository,
                fileStorage
        );
    }

    private record Fixtures(
            UUID jobId,
            Resource resource,
            AgentJobRepository agentJobRepository,
            ResourceRepository resourceRepository,
            ResourceFileRepository resourceFileRepository,
            ResourceSummaryRepository resourceSummaryRepository,
            AiDocumentRepository aiDocumentRepository,
            FileStorage fileStorage
    ) {
    }
}
