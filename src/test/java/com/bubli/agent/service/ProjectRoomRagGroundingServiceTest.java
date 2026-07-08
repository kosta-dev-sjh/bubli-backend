package com.bubli.agent.service;

import com.bubli.agent.config.AgentRagProperties;
import com.bubli.agent.type.AgentCommandMode;
import com.bubli.resource.dto.ResourceSearchHit;
import com.bubli.resource.service.ResourceSemanticSearchPublicService;
import com.bubli.resource.type.ResourceSearchScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectRoomRagGroundingServiceTest {

	@Test
	void retrievesRoomSharedHitsUsingConfiguredTopKAndThreshold() {
		ResourceSemanticSearchPublicService searchService = mock(ResourceSemanticSearchPublicService.class);
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		ResourceSearchHit low = hit(resourceId, "low similarity", 0.71D);
		ResourceSearchHit high = hit(resourceId, "payment is due in seven days", 0.9D);

		when(searchService.search(userId, ResourceSearchScope.ROOM_SHARED, roomId, "payment", 3))
				.thenReturn(List.of(low, high));

		var context = new ProjectRoomRagGroundingService(
				searchService,
				new AgentRagProperties(true, 3, 0.72D, 0.0D, 0.72D)
		).retrieve(userId, roomId, "payment", "en-US");

		verify(searchService).search(eq(userId), eq(ResourceSearchScope.ROOM_SHARED), eq(roomId), eq("payment"), eq(3));
		assertThat(context.grounded()).isTrue();
		assertThat(context.hits()).containsExactly(high);
		assertThat(context.maxSimilarity()).isEqualTo(0.9D);
		assertThat(context.promptBlock()).contains("resourceId=%s".formatted(resourceId));
		assertThat(context.promptBlock()).contains("chunkIndex=0");
		assertThat(context.promptBlock()).contains("pageNumber=2");
		assertThat(context.promptBlock()).contains("payment is due in seven days");
	}

	@Test
	void returnsUngroundedWhenSearchThrows() {
		ResourceSemanticSearchPublicService searchService = mock(ResourceSemanticSearchPublicService.class);
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();

		when(searchService.search(userId, ResourceSearchScope.ROOM_SHARED, roomId, "query", 5))
				.thenThrow(new IllegalStateException("EmbeddingModel is not available"));

		var context = new ProjectRoomRagGroundingService(
				searchService,
				new AgentRagProperties(true, 5, 0.72D, 0.0D, 0.72D)
		).retrieve(userId, roomId, "query", "ko-KR");

		assertThat(context.grounded()).isFalse();
		assertThat(context.hits()).isEmpty();
		assertThat(context.promptBlock()).isEmpty();
	}

	@Test
	void suggestModeUsesLowerSimilarityThreshold() {
		ResourceSemanticSearchPublicService searchService = mock(ResourceSemanticSearchPublicService.class);
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		ResourceSearchHit low = hit(resourceId, "contract text that can be converted into TODO", 0.31D);

		when(searchService.search(userId, ResourceSearchScope.ROOM_SHARED, roomId, "make todo", 5))
				.thenReturn(List.of(low));

		var context = new ProjectRoomRagGroundingService(
				searchService,
				new AgentRagProperties(true, 5, 0.72D, 0.0D, 0.72D)
		).retrieve(userId, roomId, "make todo", "en-US", AgentCommandMode.SUGGEST);

		assertThat(context.grounded()).isTrue();
		assertThat(context.hits()).containsExactly(low);
		assertThat(context.promptBlock()).contains("contract text that can be converted into TODO");
	}

	@Test
	void answerModeKeepsStrictSimilarityThreshold() {
		ResourceSemanticSearchPublicService searchService = mock(ResourceSemanticSearchPublicService.class);
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		ResourceSearchHit low = hit(UUID.randomUUID(), "weak hit", 0.31D);

		when(searchService.search(userId, ResourceSearchScope.ROOM_SHARED, roomId, "question", 5))
				.thenReturn(List.of(low));

		var context = new ProjectRoomRagGroundingService(
				searchService,
				new AgentRagProperties(true, 5, 0.72D, 0.0D, 0.72D)
		).retrieve(userId, roomId, "question", "en-US", AgentCommandMode.ANSWER);

		assertThat(context.grounded()).isFalse();
		assertThat(context.hits()).isEmpty();
	}

	@Test
	void returnsUngroundedWhenDisabled() {
		ResourceSemanticSearchPublicService searchService = mock(ResourceSemanticSearchPublicService.class);

		var context = new ProjectRoomRagGroundingService(
				searchService,
				new AgentRagProperties(false, 5, 0.72D, 0.0D, 0.72D)
		).retrieve(UUID.randomUUID(), UUID.randomUUID(), "query", "ko-KR");

		assertThat(context.grounded()).isFalse();
		assertThat(context.hits()).isEmpty();
	}

	private ResourceSearchHit hit(UUID resourceId, String chunkText, double similarityScore) {
		return new ResourceSearchHit(
				UUID.randomUUID(),
				resourceId,
				0,
				chunkText,
				2,
				10,
				12,
				120,
				260,
				"contract.pdf",
				"{\"pageNumber\":2,\"startLine\":10,\"endLine\":12,\"startOffset\":120,\"endOffset\":260,\"originalName\":\"contract.pdf\"}",
				similarityScore
		);
	}
}
