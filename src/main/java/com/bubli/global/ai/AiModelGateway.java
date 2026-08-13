package com.bubli.global.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AiModelGateway {

	private final ObjectProvider<ChatModel> chatModelProvider;
	private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
	private final AiCallExecutor aiCallExecutor;

	public boolean isChatAvailable() {
		return chatModelProvider.getIfAvailable() != null;
	}

	public boolean isEmbeddingAvailable() {
		return embeddingModelProvider.getIfAvailable() != null;
	}

	public String callChat(String operationName, String prompt) {
		ChatModel chatModel = chatModelProvider.getIfAvailable();
		if (chatModel == null) {
			throw new IllegalStateException("ChatModel is not available. Enable the ai profile to call the model.");
		}
		return aiCallExecutor.execute(operationName, () -> chatModel.call(prompt));
	}

	public float[] embed(String operationName, String text) {
		EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
		if (embeddingModel == null) {
			throw new IllegalStateException("EmbeddingModel is not available. Enable the ai profile to embed text.");
		}
		return aiCallExecutor.execute(operationName, () -> embeddingModel.embed(text));
	}

	public List<float[]> embedAll(String operationName, List<String> texts) {
		if (texts == null || texts.isEmpty()) {
			return List.of();
		}
		EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
		if (embeddingModel == null) {
			throw new IllegalStateException("EmbeddingModel is not available. Enable the ai profile to embed text.");
		}
		List<String> inputs = List.copyOf(texts);
		return aiCallExecutor.execute(operationName, () -> validateEmbeddings(
				embeddingModel.embed(inputs),
				inputs.size()
		));
	}

	private List<float[]> validateEmbeddings(List<float[]> embeddings, int expectedSize) {
		if (embeddings == null || embeddings.size() != expectedSize || embeddings.stream().anyMatch(java.util.Objects::isNull)) {
			throw new IllegalStateException(
					"EmbeddingModel returned %s vectors for %d inputs."
							.formatted(embeddings == null ? "null" : embeddings.size(), expectedSize)
			);
		}
		return List.copyOf(embeddings);
	}
}
