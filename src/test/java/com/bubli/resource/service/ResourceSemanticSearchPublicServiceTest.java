package com.bubli.resource.service;

import com.bubli.project.service.ProjectRoomAccessPublicService;
import com.bubli.resource.dto.ResourceSearchHit;
import com.bubli.resource.entity.ResourceEmbeddingSearchRow;
import com.bubli.resource.repository.ResourceEmbeddingRepository;
import com.bubli.resource.type.ResourceSearchScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceSemanticSearchPublicServiceTest {

    @Test
    void searchesRoomSharedEmbeddingsAfterAccessCheck() {
        ResourceEmbeddingRepository repository = mock(ResourceEmbeddingRepository.class);
        ProjectRoomAccessPublicService accessService = mock(ProjectRoomAccessPublicService.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID embeddingId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        when(embeddingModel.embed("contract payment")).thenReturn(vector(0.1f));
        when(repository.searchRoomShared(eq(roomId), anyString(), eq(3)))
                .thenReturn(List.of(row(embeddingId, resourceId, "{\"pageNumber\":2}")));

        List<ResourceSearchHit> hits = service(repository, accessService, embeddingModel)
                .search(userId, ResourceSearchScope.ROOM_SHARED, roomId, " contract payment ", 3);

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).embeddingId()).isEqualTo(embeddingId);
        assertThat(hits.get(0).resourceId()).isEqualTo(resourceId);
        assertThat(hits.get(0).pageNumber()).isEqualTo(2);
        verify(accessService).requireRoomMember(roomId, userId);
        verify(repository).searchRoomShared(eq(roomId), org.mockito.ArgumentMatchers.startsWith("[0.1,0.1"), eq(3));
    }

    @Test
    void searchesPersonalEmbeddingsByOwner() {
        ResourceEmbeddingRepository repository = mock(ResourceEmbeddingRepository.class);
        ProjectRoomAccessPublicService accessService = mock(ProjectRoomAccessPublicService.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        UUID userId = UUID.randomUUID();

        when(embeddingModel.embed(anyString())).thenReturn(vector(0.1f));
        when(repository.searchPersonal(eq(userId), anyString(), eq(5))).thenReturn(List.of());

        service(repository, accessService, embeddingModel)
                .search(userId, ResourceSearchScope.PERSONAL, null, "query", 5);

        verify(repository).searchPersonal(eq(userId), anyString(), eq(5));
    }

    @Test
    void capsTopKAtTwenty() {
        ResourceEmbeddingRepository repository = mock(ResourceEmbeddingRepository.class);
        ProjectRoomAccessPublicService accessService = mock(ProjectRoomAccessPublicService.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        when(embeddingModel.embed(anyString())).thenReturn(vector(0.1f));
        when(repository.searchRoomShared(eq(roomId), anyString(), eq(20))).thenReturn(List.of());

        service(repository, accessService, embeddingModel)
                .search(userId, ResourceSearchScope.ROOM_SHARED, roomId, "query", 100);

        verify(repository).searchRoomShared(eq(roomId), anyString(), eq(20));
    }

    @Test
    void failsWhenEmbeddingModelIsUnavailable() {
        ResourceSemanticSearchPublicService service = service(
                mock(ResourceEmbeddingRepository.class),
                mock(ProjectRoomAccessPublicService.class),
                null
        );

        assertThatThrownBy(() -> service.search(UUID.randomUUID(), ResourceSearchScope.PERSONAL, null, "query", 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EmbeddingModel is not available");
    }

    private ResourceSemanticSearchPublicService service(
            ResourceEmbeddingRepository repository,
            ProjectRoomAccessPublicService accessService,
            EmbeddingModel embeddingModel
    ) {
        return new ResourceSemanticSearchPublicService(
                repository,
                mockProvider(embeddingModel),
                new EmbeddingVectorFormatter(),
                accessService,
                new ObjectMapper()
        );
    }

    private ResourceEmbeddingSearchRow row(UUID embeddingId, UUID resourceId, String metadata) {
        return new ResourceEmbeddingSearchRow() {
            @Override
            public UUID getId() {
                return embeddingId;
            }

            @Override
            public UUID getResourceId() {
                return resourceId;
            }

            @Override
            public int getChunkIndex() {
                return 0;
            }

            @Override
            public String getChunkText() {
                return "contract payment within 7 days";
            }

            @Override
            public String getChunkMetadata() {
                return metadata;
            }

            @Override
            public double getSimilarityScore() {
                return 0.91;
            }
        };
    }

    private ObjectProvider<EmbeddingModel> mockProvider(EmbeddingModel embeddingModel) {
        @SuppressWarnings("unchecked")
        ObjectProvider<EmbeddingModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(embeddingModel);
        return provider;
    }

    private float[] vector(float value) {
        float[] vector = new float[1024];
        java.util.Arrays.fill(vector, value);
        return vector;
    }
}
