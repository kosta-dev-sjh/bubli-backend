package com.bubli.resource.service;

import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceEmbedding;
import com.bubli.resource.entity.ResourceRelation;
import com.bubli.resource.repository.ResourceEmbeddingRepository;
import com.bubli.resource.repository.ResourceRelationRepository;
import com.bubli.resource.repository.ResourceRepository;
import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.resource.type.ResourceVisibility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class ResourceRelationIndexPublicServiceTest {

	@Mock
	ResourceEmbeddingRepository resourceEmbeddingRepository;

	@Mock
	ResourceRelationRepository resourceRelationRepository;

	@Mock
	ResourceRepository resourceRepository;

	@InjectMocks
	ResourceRelationIndexPublicService service;

	@Test
	void rebuildRelationsCreatesBidirectionalRelationsForSimilarPersonalResources() {
		UUID ownerId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		UUID similarResourceId = UUID.randomUUID();
		UUID weakResourceId = UUID.randomUUID();
		Resource resource = resource(resourceId, ownerId, null, ResourceVisibility.PERSONAL);
		Resource similarResource = resource(similarResourceId, ownerId, null, ResourceVisibility.PERSONAL);
		Resource weakResource = resource(weakResourceId, ownerId, null, ResourceVisibility.PERSONAL);

		ResourceEmbedding source = embedding(resourceId, ownerId, null, ResourceVisibility.PERSONAL, "[1,0,0]");
		ResourceEmbedding self = embedding(resourceId, ownerId, null, ResourceVisibility.PERSONAL, "[1,0,0]");
		ResourceEmbedding similar = embedding(similarResourceId, ownerId, null, ResourceVisibility.PERSONAL, "[0.9,0.1,0]");
		ResourceEmbedding weak = embedding(weakResourceId, ownerId, null, ResourceVisibility.PERSONAL, "[0,1,0]");
		given(resourceEmbeddingRepository.findAllByResourceIdOrderByChunkIndex(resourceId)).willReturn(List.of(source));
		given(resourceEmbeddingRepository.findAllByOwnerIdAndVisibility(ownerId, ResourceVisibility.PERSONAL))
				.willReturn(List.of(self, similar, weak));
		given(resourceRepository.findByIdAndDeletedAtIsNull(similarResourceId)).willReturn(Optional.of(similarResource));
		given(resourceRepository.findByIdAndDeletedAtIsNull(weakResourceId)).willReturn(Optional.of(weakResource));

		ResourceRelationIndexPublicService.RelationIndexResult result = service.rebuildRelations(resource);

		assertThat(result.relationCount()).isEqualTo(2);
		verify(resourceRelationRepository).deleteByResourceIdOrRelatedResourceId(resourceId, resourceId);
		ArgumentCaptor<Iterable> captor = ArgumentCaptor.forClass(Iterable.class);
		verify(resourceRelationRepository).saveAll(captor.capture());
		List<ResourceRelation> relations = toList(captor.getValue());
		assertThat(relations).hasSize(2);
		assertThat(relations)
				.extracting(ResourceRelation::getResourceId, ResourceRelation::getRelatedResourceId)
				.containsExactlyInAnyOrder(
						org.assertj.core.groups.Tuple.tuple(resourceId, similarResourceId),
						org.assertj.core.groups.Tuple.tuple(similarResourceId, resourceId)
				);
		assertThat(relations).allSatisfy(relation -> {
			assertThat(relation.getReason()).isEqualTo("SIMILAR_CONTENT");
			assertThat(relation.getScore()).isGreaterThan(new BigDecimal("0.99"));
		});
	}

	@Test
	void rebuildRelationsUsesRoomSharedScopeForRoomResources() {
		UUID ownerId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		Resource resource = resource(resourceId, ownerId, roomId, ResourceVisibility.ROOM_SHARED);
		ResourceEmbedding source = embedding(resourceId, ownerId, roomId, ResourceVisibility.ROOM_SHARED, "[1,0]");
		given(resourceEmbeddingRepository.findAllByResourceIdOrderByChunkIndex(resourceId)).willReturn(List.of(source));
		given(resourceEmbeddingRepository.findAllByRoomIdAndVisibility(roomId, ResourceVisibility.ROOM_SHARED))
				.willReturn(List.of(source));

		ResourceRelationIndexPublicService.RelationIndexResult result = service.rebuildRelations(resource);

		assertThat(result.relationCount()).isZero();
		verify(resourceRelationRepository).deleteByResourceIdOrRelatedResourceId(resourceId, resourceId);
		verify(resourceRelationRepository).saveAll(any());
	}

	private Resource resource(UUID id, UUID ownerId, UUID roomId, ResourceVisibility visibility) {
		Resource resource = Resource.create(ownerId, roomId, "resource", ResourceKind.FILE, visibility, ResourceStatus.ANALYZED);
		ReflectionTestUtils.setField(resource, "id", id);
		return resource;
	}

	private ResourceEmbedding embedding(
			UUID resourceId,
			UUID ownerId,
			UUID roomId,
			ResourceVisibility visibility,
			String vector
	) {
		return ResourceEmbedding.create(
				resourceId,
				ownerId,
				roomId,
				visibility,
				0,
				"chunk",
				vector,
				null
		);
	}

	private List<ResourceRelation> toList(Iterable relations) {
		return ((List<?>) relations).stream()
				.map(ResourceRelation.class::cast)
				.toList();
	}
}
