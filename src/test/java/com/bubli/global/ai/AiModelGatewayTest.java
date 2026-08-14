package com.bubli.global.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiModelGatewayTest {

	@Test
	void routesChatAndEmbeddingThroughConfiguredModels() {
		ChatModel chatModel = mock(ChatModel.class);
		EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
		when(chatModel.call("prompt")).thenReturn("answer");
		when(embeddingModel.embed("query")).thenReturn(new float[]{0.1F, 0.2F});
		AiModelGateway gateway = new AiModelGateway(
				provider(chatModel),
				provider(embeddingModel),
				new AiCallExecutor(1, Duration.ZERO)
		);

		assertThat(gateway.callChat("chat-test", "prompt")).isEqualTo("answer");
		assertThat(gateway.embed("embedding-test", "query")).containsExactly(0.1F, 0.2F);
		verify(chatModel).call("prompt");
		verify(embeddingModel).embed("query");
	}

	@Test
	void batchesEmbeddingInputsThroughOneModelCall() {
		EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
		List<String> inputs = List.of("first", "second");
		when(embeddingModel.embed(inputs)).thenReturn(List.of(
				new float[]{0.1F, 0.2F},
				new float[]{0.3F, 0.4F}
		));
		AiModelGateway gateway = new AiModelGateway(
				provider(null),
				provider(embeddingModel),
				new AiCallExecutor(1, Duration.ZERO)
		);

		assertThat(gateway.embedAll("embedding-batch-test", inputs))
				.hasSize(2)
				.element(1)
				.satisfies(vector -> assertThat(vector).containsExactly(0.3F, 0.4F));
		verify(embeddingModel).embed(inputs);
	}

	@Test
	void rejectsIncompleteBatchEmbeddingResponses() {
		EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
		List<String> inputs = List.of("first", "second");
		when(embeddingModel.embed(inputs)).thenReturn(List.of(new float[]{0.1F}));
		AiModelGateway gateway = new AiModelGateway(
				provider(null),
				provider(embeddingModel),
				new AiCallExecutor(1, Duration.ZERO)
		);

		assertThatThrownBy(() -> gateway.embedAll("embedding-batch-test", inputs))
				.isInstanceOf(AiCallFailedException.class)
				.hasCauseInstanceOf(IllegalStateException.class);
	}

	@Test
	void reportsUnavailableModelWithoutInvokingAnOperation() {
		AiModelGateway gateway = new AiModelGateway(
				provider(null),
				provider(null),
				new AiCallExecutor(1, Duration.ZERO)
		);

		assertThat(gateway.isChatAvailable()).isFalse();
		assertThat(gateway.isEmbeddingAvailable()).isFalse();
		assertThatThrownBy(() -> gateway.embed("embedding-test", "query"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("EmbeddingModel is not available");
	}

	@SuppressWarnings("unchecked")
	private <T> ObjectProvider<T> provider(T value) {
		ObjectProvider<T> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(value);
		return provider;
	}
}
