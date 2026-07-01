package com.bubli.agent.dto;

import java.nio.charset.StandardCharsets;

public record GeneratedDocumentExportResult(
        String fileName,
        String contentType,
        byte[] content
) {

    private static final String MARKDOWN_CONTENT_TYPE = "text/markdown;charset=UTF-8";

    public static GeneratedDocumentExportResult markdown(String title, String contentMarkdown) {
        String fileName = safeFileName(title) + ".md";
        return new GeneratedDocumentExportResult(
                fileName,
                MARKDOWN_CONTENT_TYPE,
                contentMarkdown.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String safeFileName(String title) {
        if (title == null || title.isBlank()) {
            return "generated-document";
        }
        String safe = title.trim()
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", " ");
        if (safe.isBlank()) {
            return "generated-document";
        }
        return safe.length() > 80 ? safe.substring(0, 80).trim() : safe;
    }
}
