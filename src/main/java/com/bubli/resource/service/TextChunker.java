package com.bubli.resource.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private static final int MAX_CHUNK_CHARS = 1200;
    private static final int CHUNK_OVERLAP_CHARS = 200;

    public List<TextChunk> split(String text) {
        return splitPages(List.of(new TextPage(null, text)));
    }

    public List<TextChunk> splitPages(List<TextPage> pages) {
        if (pages == null || pages.isEmpty()) {
            return List.of();
        }

        List<TextChunk> chunks = new ArrayList<>();
        for (TextPage page : pages) {
            splitPage(page, chunks);
        }
        return chunks;
    }

    private void splitPage(TextPage page, List<TextChunk> chunks) {
        if (page == null) {
            return;
        }
        Integer pageNumber = page.pageNumber();
        String text = page.text();
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return;
        }

        int start = 0;
        while (start < normalized.length()) {
            int end = preferredEnd(normalized, start);
            String chunkText = normalized.substring(start, end).trim();
            if (!chunkText.isBlank()) {
                chunks.add(new TextChunk(chunks.size(), chunkText, start, end, pageNumber));
            }
            if (end == normalized.length()) {
                break;
            }
            start = Math.max(0, end - CHUNK_OVERLAP_CHARS);
        }
    }

    private int preferredEnd(String text, int start) {
        int hardEnd = Math.min(start + MAX_CHUNK_CHARS, text.length());
        if (hardEnd == text.length()) {
            return hardEnd;
        }

        int paragraphBreak = text.lastIndexOf("\n\n", hardEnd);
        if (paragraphBreak > start + CHUNK_OVERLAP_CHARS) {
            return paragraphBreak;
        }

        int sentenceBreak = Math.max(
                Math.max(text.lastIndexOf(". ", hardEnd), text.lastIndexOf("? ", hardEnd)),
                text.lastIndexOf("! ", hardEnd)
        );
        if (sentenceBreak > start + CHUNK_OVERLAP_CHARS) {
            return sentenceBreak + 1;
        }

        int whitespace = text.lastIndexOf(' ', hardEnd);
        if (whitespace > start + CHUNK_OVERLAP_CHARS) {
            return whitespace;
        }

        return hardEnd;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \t\f]+", " ")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    public record TextChunk(
            int index,
            String text,
            int startOffset,
            int endOffset,
            Integer pageNumber
    ) {
    }

    public record TextPage(
            Integer pageNumber,
            String text
    ) {
    }
}
