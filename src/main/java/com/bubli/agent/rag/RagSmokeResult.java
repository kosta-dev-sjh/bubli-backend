package com.bubli.agent.rag;

import java.util.List;

public record RagSmokeResult(
        String query,
        boolean chatModelResponded,
        int embeddingDimensions,
        int indexedDocumentCount,
        int matchedDocumentCount,
        List<String> matchedDocumentIds
) {

    public RagSmokeResult {
        matchedDocumentIds = List.copyOf(matchedDocumentIds);
    }
}
