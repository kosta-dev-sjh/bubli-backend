package com.bubli.agent.rag;

import com.bubli.agent.model.AiCallExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Profile("ai")
@RequiredArgsConstructor
public class RagSmokeService {

    private static final String SMOKE_PROJECT_ROOM_ID = "rag-smoke-project";
    private static final String CONTRACT_DOCUMENT_ID = "00000000-0000-0000-0000-000000000101";
    private static final String MEETING_DOCUMENT_ID = "00000000-0000-0000-0000-000000000102";

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final AiCallExecutor aiCallExecutor;

    public RagSmokeResult run() {
        String chatResponse = aiCallExecutor.execute(
                "bedrock-chat-smoke",
                () -> chatModel.call("반드시 OK 한 단어로만 답하세요.")
        );
        float[] embedding = aiCallExecutor.execute(
                "bedrock-embedding-smoke",
                () -> embeddingModel.embed("Bubli RAG 연결 확인")
        );

        List<Document> documents = createDocuments();
        List<String> documentIds = documents.stream().map(Document::getId).toList();

        try {
            aiCallExecutor.execute("pgvector-add-smoke", () -> {
                vectorStore.add(documents);
                return null;
            });

            String query = "계약 대금은 언제 지급해야 하나요?";
            List<Document> matches = aiCallExecutor.execute(
                    "pgvector-search-smoke",
                    () -> vectorStore.similaritySearch(
                            SearchRequest.builder()
                                    .query(query)
                                    .topK(2)
                                    .similarityThreshold(0.0)
                                    .filterExpression("projectRoomId == '" + SMOKE_PROJECT_ROOM_ID + "'")
                                    .build()
                    )
            );

            return new RagSmokeResult(
                    query,
                    chatResponse != null && !chatResponse.isBlank(),
                    embedding.length,
                    documents.size(),
                    matches.size(),
                    matches.stream().map(Document::getId).toList()
            );
        } finally {
            vectorStore.delete(documentIds);
        }
    }

    private List<Document> createDocuments() {
        return List.of(
                new Document(
                        CONTRACT_DOCUMENT_ID,
                        "계약 대금은 최종 납품물 검수 완료 후 7일 이내에 지급한다.",
                        metadata("CONTRACT", 1)
                ),
                new Document(
                        MEETING_DOCUMENT_ID,
                        "프로젝트 정기 회의는 매주 월요일 오전 10시에 진행한다.",
                        metadata("MEETING_NOTE", 2)
                )
        );
    }

    private Map<String, Object> metadata(String documentType, int pageNumber) {
        return Map.of(
                "projectRoomId", SMOKE_PROJECT_ROOM_ID,
                "ownerId", "rag-smoke-user",
                "scope", "PROJECT",
                "documentType", documentType,
                "documentStatus", "READY",
                "documentVersion", 1,
                "deleted", false,
                "pageNumber", pageNumber
        );
    }
}
