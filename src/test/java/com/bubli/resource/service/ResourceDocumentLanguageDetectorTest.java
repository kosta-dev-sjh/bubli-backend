package com.bubli.resource.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceDocumentLanguageDetectorTest {

    @Test
    void detectsKoreanEnglishAndJapaneseSourceText() {
        assertThat(ResourceDocumentLanguageDetector.detect("프로젝트 요구사항을 확인합니다."))
                .isEqualTo("ko");
        assertThat(ResourceDocumentLanguageDetector.detect("The project requirement is approved."))
                .isEqualTo("en");
        assertThat(ResourceDocumentLanguageDetector.detect("予約要件を確認してください。"))
                .isEqualTo("ja");
    }
}
