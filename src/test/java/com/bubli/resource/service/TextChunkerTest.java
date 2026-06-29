package com.bubli.resource.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

    private final TextChunker textChunker = new TextChunker();

    @Test
    void splitsLongTextIntoOverlappingChunks() {
        String text = "요구사항 ".repeat(300);

        List<TextChunker.TextChunk> chunks = textChunker.split(text);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks.get(0).index()).isZero();
        assertThat(chunks.get(1).startOffset()).isLessThan(chunks.get(0).endOffset());
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.text()).isNotBlank());
    }

    @Test
    void returnsEmptyListForBlankText() {
        assertThat(textChunker.split("   \n\t  ")).isEmpty();
    }

    @Test
    void preservesPageNumberWhenSplittingPages() {
        List<TextChunker.TextChunk> chunks = textChunker.splitPages(List.of(
                new TextChunker.TextPage(1, "first page ".repeat(200)),
                new TextChunker.TextPage(2, "second page")
        ));

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).extracting(TextChunker.TextChunk::pageNumber).contains(1, 2);
        assertThat(chunks).extracting(TextChunker.TextChunk::index)
                .containsExactly(java.util.stream.IntStream.range(0, chunks.size()).boxed().toArray(Integer[]::new));
    }
}
