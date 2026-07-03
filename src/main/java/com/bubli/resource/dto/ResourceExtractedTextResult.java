package com.bubli.resource.dto;

import com.bubli.resource.entity.ResourceExtractedText;

import java.util.UUID;

public record ResourceExtractedTextResult(
        UUID id,
        UUID resourceId,
        String localFileId,
        String extractionMethod
) {
    public static ResourceExtractedTextResult from(ResourceExtractedText extractedText) {
        return new ResourceExtractedTextResult(
                extractedText.getId(),
                extractedText.getResourceId(),
                extractedText.getLocalFileId(),
                extractedText.getExtractionMethod()
        );
    }
}
