package com.bubli.resource.dto;

import com.bubli.resource.type.DocumentType;

import java.util.List;
import java.util.UUID;

public record ResourceAnalysisSource(
        UUID resourceId,
        UUID roomId,
        String originalName,
        String mimeType,
        DocumentType documentType,
        List<ResourceAnalysisPage> pages,
        String text,
        int pageCount,
        int characterCount
) {

    public ResourceAnalysisSource {
        pages = pages == null ? List.of() : List.copyOf(pages);
    }
}
