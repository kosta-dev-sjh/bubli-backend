package com.bubli.agent.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentQuerySupportTest {

	@Test
	void analyzesKoreanRequirementIdAndKeywords() {
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze(
				"/bubli 공공도서관 문서에서 REQ-LB-007 해당 기능은 어떤 것을 말하는거야?",
				"ko-KR"
		);

		assertThat(analysis.locale()).isEqualTo("ko-KR");
		assertThat(analysis.requirementIdentifiers()).containsExactly("req-lb-007");
		assertThat(analysis.keywords()).contains("req-lb-007", "공공도서관");
		assertThat(analysis.normalizedQuery()).contains("req-lb-007");
	}

	@Test
	void analyzesEnglishQuotedPhrase() {
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze(
				"Find the source for \"seat reservation status\" in the requirements document",
				"en-US"
		);

		assertThat(analysis.quotedPhrases()).containsExactly("seat reservation status");
		assertThat(analysis.keywords()).contains("seat", "reservation");
		assertThat(AgentQuerySupport.isDocumentSourceRequest("requirements document")).isTrue();
	}

	@Test
	void analyzesJapaneseDocumentAndInventoryIntent() {
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze(
				"要件定義書で座席予約について説明して",
				"ja-JP"
		);

		assertThat(analysis.locale()).isEqualTo("ja-JP");
		assertThat(analysis.keywords()).contains("要件定義書", "座席予約");
		assertThat(analysis.keywords()).doesNotContain("要件定義書で座席予約について説明して");
		assertThat(AgentQuerySupport.isJapaneseLocale(analysis.locale())).isTrue();
		assertThat(AgentQuerySupport.isDocumentSourceRequest("要件定義書で座席予約について説明して")).isTrue();
		assertThat(AgentQuerySupport.requiresSemanticDocumentEvidence("要件定義書で座席予約について説明して")).isTrue();
	}
}
