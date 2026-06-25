package com.bubli.agent.rag;

import com.bubli.agent.model.AiCallExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagSmokeServiceTest {

    @Test
    void checksChatEmbeddingAndProjectFilteredVectorSearch() {
        ChatModel chatModel = mock(ChatModel.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        VectorStore vectorStore = mock(VectorStore.class);
        when(chatModel.call(any(String.class))).thenReturn("OK");
        when(embeddingModel.embed(any(String.class))).thenReturn(new float[1024]);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("matched contract text")));

        RagSmokeResult result = new RagSmokeService(
                chatModel,
                embeddingModel,
                vectorStore,
                new AiCallExecutor(1, Duration.ZERO)
        ).run();

        assertThat(result.chatModelResponded()).isTrue();
        assertThat(result.embeddingDimensions()).isEqualTo(1024);
        assertThat(result.indexedDocumentCount()).isEqualTo(2);
        assertThat(result.matchedDocumentCount()).isEqualTo(1);
        verify(vectorStore).add(any());
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
        verify(vectorStore).delete(List.of(
                "00000000-0000-0000-0000-000000000101",
                "00000000-0000-0000-0000-000000000102"
        ));
    }
}
