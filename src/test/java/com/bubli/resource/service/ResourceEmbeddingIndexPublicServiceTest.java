package com.bubli.resource.service;

import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceFile;
import com.bubli.resource.repository.ResourceEmbeddingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceEmbeddingIndexPublicServiceTest {

    @Test
    void indexesChunksWhenEmbeddingModelIsAvailable() {
        ResourceEmbeddingRepository repository = mock(ResourceEmbeddingRepository.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        ObjectProvider<EmbeddingModel> provider = mockProvider(embeddingModel);
        Resource resource = resource();
        ResourceFile resourceFile = resourceFile(resource.getId());

        when(embeddingModel.embed(anyString())).thenReturn(vector(0.25f));

        ResourceEmbeddingIndexPublicService.IndexResult result = service(repository, provider)
                .index(resource, resourceFile, "requirements ".repeat(300));

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

        when(embeddingModel.embed(anyString())).thenReturn(vector(0.25f));

        service(repository, provider)
                .index(resource, resourceFile, List.of(new TextChunker.TextPage(3, "page text")));

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

        ResourceEmbeddingIndexPublicService.IndexResult result = service(repository, provider)
                .index(resource(), resourceFile(UUID.randomUUID()), "text");

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

    private ResourceEmbeddingIndexPublicService service(
            ResourceEmbeddingRepository repository,
            ObjectProvider<EmbeddingModel> provider
    ) {
        return new ResourceEmbeddingIndexPublicService(
                repository,
                new TextChunker(),
                provider,
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

}
