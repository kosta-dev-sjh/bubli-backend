package com.bubli.resource.service;

import com.bubli.global.ai.AiCallExecutor;
import com.bubli.global.ai.AiModelGateway;
import com.bubli.resource.dto.PreparedResourceEmbeddingIndex;
import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceFile;
import com.bubli.resource.repository.ResourceEmbeddingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResourceEmbeddingIndexPublicServiceTest {

    @Test
    void preparesRemoteEmbeddingsBeforeReplacingDatabaseRows() {
        ResourceEmbeddingRepository repository = mock(ResourceEmbeddingRepository.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        Resource resource = resource();
        ResourceFile resourceFile = resourceFile(resource.getId());
        stubBatchEmbeddingModel(embeddingModel, 0.25f);

        ResourceEmbeddingIndexPublicService service = service(repository, mockProvider(embeddingModel));
        PreparedResourceEmbeddingIndex preparedIndex = service.prepare(
                resource,
                resourceFile,
                List.of(new TextChunker.TextPage(1, "payment requirements"))
        );

        assertThat(preparedIndex.indexed()).isTrue();
        assertThat(preparedIndex.chunkCount()).isEqualTo(1);
        verify(embeddingModel).embed(List.of("payment requirements"));
        verifyNoInteractions(repository);

        ResourceEmbeddingIndexPublicService.IndexResult result = service.replace(preparedIndex);

        assertThat(result.indexed()).isTrue();
        verify(repository).deleteAllByResourceId(resource.getId());
        verify(repository).insertEmbedding(
                any(UUID.class),
                eq(resource.getId()),
                eq(resource.getOwnerId()),
                eq(resource.getRoomId()),
                eq(resource.getVisibility().name()),
                eq(0),
                eq("payment requirements"),
                org.mockito.ArgumentMatchers.startsWith("[0.25,0.25"),
                org.mockito.ArgumentMatchers.contains("\"documentLanguage\":\"en\"")
        );
    }

    @Test
    void detectsLanguagePerChunkForMixedLanguageDocuments() {
        ResourceEmbeddingRepository repository = mock(ResourceEmbeddingRepository.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        Resource resource = resource();
        ResourceFile resourceFile = resourceFile(resource.getId());
        stubBatchEmbeddingModel(embeddingModel, 0.25f);

        ResourceEmbeddingIndexPublicService service = service(repository, mockProvider(embeddingModel));
        PreparedResourceEmbeddingIndex preparedIndex = service.prepare(
                resource,
                resourceFile,
                List.of(
                        new TextChunker.TextPage(1, "계약 조건과 납기일을 확인합니다."),
                        new TextChunker.TextPage(2, "Review the payment terms and delivery date.")
                )
        );
        service.replace(preparedIndex);

        ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);
        verify(repository, times(2)).insertEmbedding(
                any(UUID.class),
                eq(resource.getId()),
                eq(resource.getOwnerId()),
                eq(resource.getRoomId()),
                eq(resource.getVisibility().name()),
                org.mockito.ArgumentMatchers.anyInt(),
                anyString(),
                anyString(),
                metadataCaptor.capture()
        );
        assertThat(metadataCaptor.getAllValues())
                .anyMatch(metadata -> metadata.contains("\"documentLanguage\":\"ko\""))
                .anyMatch(metadata -> metadata.contains("\"documentLanguage\":\"en\""));
    }

    @Test
    void indexesChunksWhenEmbeddingModelIsAvailable() {
        ResourceEmbeddingRepository repository = mock(ResourceEmbeddingRepository.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        ObjectProvider<EmbeddingModel> provider = mockProvider(embeddingModel);
        Resource resource = resource();
        ResourceFile resourceFile = resourceFile(resource.getId());

        stubBatchEmbeddingModel(embeddingModel, 0.25f);

        ResourceEmbeddingIndexPublicService service = service(repository, provider);
        PreparedResourceEmbeddingIndex preparedIndex = service.prepare(
                resource,
                resourceFile,
                List.of(new TextChunker.TextPage(null, "requirements ".repeat(300)))
        );
        ResourceEmbeddingIndexPublicService.IndexResult result = service.replace(preparedIndex);

        assertThat(result.indexed()).isTrue();
        assertThat(result.chunkCount()).isGreaterThan(1);
        verify(repository).deleteAllByResourceId(resource.getId());
        verify(repository).insertEmbedding(
                any(UUID.class),
                eq(resource.getId()),
                eq(resource.getOwnerId()),
                eq(resource.getRoomId()),
                eq(resource.getVisibility().name()),
                eq(0),
                anyString(),
                org.mockito.ArgumentMatchers.startsWith("[0.25,0.25"),
                org.mockito.ArgumentMatchers.contains("\"originalName\":\"requirements.txt\"")
        );
    }

    @Test
    void storesPageNumberInChunkMetadata() {
        ResourceEmbeddingRepository repository = mock(ResourceEmbeddingRepository.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        ObjectProvider<EmbeddingModel> provider = mockProvider(embeddingModel);
        Resource resource = resource();
        ResourceFile resourceFile = resourceFile(resource.getId());

        stubBatchEmbeddingModel(embeddingModel, 0.25f);

        ResourceEmbeddingIndexPublicService service = service(repository, provider);
        PreparedResourceEmbeddingIndex preparedIndex = service.prepare(
                resource,
                resourceFile,
                List.of(new TextChunker.TextPage(3, "page text"))
        );
        service.replace(preparedIndex);

        verify(repository).insertEmbedding(
                any(UUID.class),
                eq(resource.getId()),
                eq(resource.getOwnerId()),
                eq(resource.getRoomId()),
                eq(resource.getVisibility().name()),
                eq(0),
                anyString(),
                org.mockito.ArgumentMatchers.startsWith("[0.25,0.25"),
                org.mockito.ArgumentMatchers.contains("\"pageNumber\":3")
        );
    }

    @Test
    void skipsIndexingWhenEmbeddingModelIsNotAvailable() {
        ResourceEmbeddingRepository repository = mock(ResourceEmbeddingRepository.class);
        ObjectProvider<EmbeddingModel> provider = mockProvider(null);

        Resource resource = resource();
        ResourceEmbeddingIndexPublicService service = service(repository, provider);
        PreparedResourceEmbeddingIndex preparedIndex = service.prepare(
                resource,
                resourceFile(resource.getId()),
                List.of(new TextChunker.TextPage(null, "text"))
        );
        ResourceEmbeddingIndexPublicService.IndexResult result = service.replace(preparedIndex);

        assertThat(result.indexed()).isFalse();
        assertThat(result.chunkCount()).isZero();
        verify(repository, never()).deleteAllByResourceId(any());
        verify(repository, never()).insertEmbedding(
                any(UUID.class),
                any(UUID.class),
                any(UUID.class),
                any(),
                anyString(),
                org.mockito.ArgumentMatchers.anyInt(),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void boundsEmbeddingRequestsToThirtyTwoChunksPerBatch() {
        ResourceEmbeddingRepository repository = mock(ResourceEmbeddingRepository.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        stubBatchEmbeddingModel(embeddingModel, 0.25f);
        Resource resource = resource();
        List<TextChunker.TextPage> pages = new ArrayList<>();
        for (int page = 1; page <= 33; page++) {
            pages.add(new TextChunker.TextPage(page, "page " + page));
        }

        service(repository, mockProvider(embeddingModel)).prepare(
                resource,
                resourceFile(resource.getId()),
                pages
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(embeddingModel, times(2)).embed(batchCaptor.capture());
        assertThat(batchCaptor.getAllValues()).extracting(List::size).containsExactly(32, 1);
    }

    private ResourceEmbeddingIndexPublicService service(
            ResourceEmbeddingRepository repository,
            ObjectProvider<EmbeddingModel> provider
    ) {
        return new ResourceEmbeddingIndexPublicService(
                repository,
                new TextChunker(),
                new AiModelGateway(
                        mock(ObjectProvider.class),
                        provider,
                        new AiCallExecutor(1, Duration.ZERO)
                ),
                new EmbeddingVectorFormatter(),
                new ObjectMapper()
        );
    }

    private ObjectProvider<EmbeddingModel> mockProvider(EmbeddingModel embeddingModel) {
        @SuppressWarnings("unchecked")
        ObjectProvider<EmbeddingModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(embeddingModel);
        return provider;
    }

    private Resource resource() {
        UUID resourceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        Resource resource = Resource.roomFile(ownerId, roomId, "requirements.txt");
        ReflectionTestUtils.setField(resource, "id", resourceId);
        return resource;
    }

    private ResourceFile resourceFile(UUID resourceId) {
        ResourceFile resourceFile = ResourceFile.create(
                resourceId,
                "requirements.txt",
                "text/plain; charset=utf-8",
                100,
                "resources/room/requirements.txt",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        );
        ReflectionTestUtils.setField(resourceFile, "id", UUID.randomUUID());
        return resourceFile;
    }

    private float[] vector(float value) {
        float[] vector = new float[1024];
        java.util.Arrays.fill(vector, value);
        return vector;
    }

    private void stubBatchEmbeddingModel(EmbeddingModel embeddingModel, float value) {
        when(embeddingModel.embed(anyList())).thenAnswer(invocation -> {
            List<?> texts = invocation.getArgument(0, List.class);
            return texts.stream().map(ignored -> vector(value)).toList();
        });
    }

}
