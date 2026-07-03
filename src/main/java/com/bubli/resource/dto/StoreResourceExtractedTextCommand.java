package com.bubli.resource.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record StoreResourceExtractedTextCommand(
        UUID resourceId,
        String localFileId,
        String checksum,
        String extractionMethod,
        int sourceCharCount,
        int analyzedCharCount,
        String combinedText,
        List<Map<String, Object>> sentencesJson,
        boolean textTruncated
) {
}
