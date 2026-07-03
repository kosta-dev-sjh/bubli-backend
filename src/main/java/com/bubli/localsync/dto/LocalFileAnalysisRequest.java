package com.bubli.localsync.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record LocalFileAnalysisRequest(
        @NotNull UUID resourceId,
        @NotBlank @Size(max = 120) String localFileId,
        @NotBlank @Size(max = 255) String fileName,
        @Size(max = 120) String mimeType,
        @Size(max = 128) String checksum,
        @NotBlank @Size(max = 80) String extractionMethod,
        @Min(0) int sourceCharCount,
        @Min(0) int analyzedCharCount,
        @NotEmpty @Size(max = 50) @Valid List<LocalFileAnalysisKeySentence> keySentences,
        @NotBlank @Size(max = 120_000) String combinedText,
        boolean textTruncated
) {
    public LocalFileAnalysisCommand toCommand() {
        return new LocalFileAnalysisCommand(
                resourceId,
                localFileId,
                fileName,
                mimeType,
                checksum,
                extractionMethod,
                sourceCharCount,
                analyzedCharCount,
                keySentences,
                combinedText,
                textTruncated
        );
    }
}
