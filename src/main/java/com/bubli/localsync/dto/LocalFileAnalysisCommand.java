package com.bubli.localsync.dto;

import java.util.List;
import java.util.UUID;

public record LocalFileAnalysisCommand(
        UUID resourceId,
        String localFileId,
        String fileName,
        String mimeType,
        String checksum,
        String extractionMethod,
        int sourceCharCount,
        int analyzedCharCount,
        List<LocalFileAnalysisKeySentence> keySentences,
        String combinedText,
        boolean textTruncated
) {
}
