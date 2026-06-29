package com.bubli.resource.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
//전체 흐름
//TextPage 리스트 입력
//     ↓
//normalize()         → 공백/줄바꿈 정규화
//     ↓
//while 루프 (슬라이딩 윈도우)
//  preferredEnd()    → 문단 > 문장 > 단어 > 강제 순서로 끝점 탐색
//  substring()       → chunk 텍스트 추출
//  TextChunk 생성    → index, text, startOffset, endOffset, pageNumber
//  start 갱신        → end - 200 (overlap 적용)
//    ↓
//List<TextChunk> 반환



@Component
public class TextChunker {
    //최대 길이
    private static final int MAX_CHUNK_CHARS = 1200;
    //오버랩 길이
    private static final int CHUNK_OVERLAP_CHARS = 200;
    //텍스트만 있을때
    public List<TextChunk> split(String text) {
        return splitPages(List.of(new TextPage(null, text)));
    }
    //페이지 정보 있을때
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
    //슬라이딩 윈도우 방식
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
    //단순 글자별로 자르는것이 아닌, (\n\n || . ? ! || 공백or단어 경계)
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
    //문단 나누는것, 공백 제거 등 하나로 정규화
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
