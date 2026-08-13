package com.bubli.agent.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentQuerySupportTest {

	@Test
	void recognizesUserProblemOverviewAndReviewCautionPhrases() {
		assertThat(AgentQuerySupport.queryIntent(
				"이 파일이 해결하려는 문제와 주요 사용자를 설명해줘"
		)).isEqualTo(ProjectRoomQueryIntent.DOCUMENT_OVERVIEW);
		assertThat(AgentQuerySupport.queryIntent(
				"좌석 예약과 대출을 테스트할 때 주의할 사항을 정리해줘"
		)).isEqualTo(ProjectRoomQueryIntent.REVIEW_CHECKLIST);
		assertThat(AgentQuerySupport.queryIntent(
				"/bubli 업무관리_프로젝트룸 파일에서 내가 중심적으로 봐야할 내용은 뭐가있어"
		)).isEqualTo(ProjectRoomQueryIntent.REVIEW_CHECKLIST);
		assertThat(AgentQuerySupport.queryIntent(
				"업무관리 프로젝트룸에서 중점적으로 봐야 할 내용은 뭐야"
		)).isEqualTo(ProjectRoomQueryIntent.REVIEW_CHECKLIST);
		assertThat(AgentQuerySupport.queryIntent(
				"백엔드 개발자 관점에서 중점적으로 봐야 할 내용은 뭐야"
		)).isEqualTo(ProjectRoomQueryIntent.ROLE_BASED_ANALYSIS);
	}

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

	@Test
	void derivesDocumentSearchLanguageFromQueryAndKeepsExplicitIdentifiersLanguageAgnostic() {
		assertThat(AgentQuerySupport.documentQueryLanguage("계약서 지급 조건을 알려줘")).isEqualTo("ko");
		assertThat(AgentQuerySupport.documentQueryLanguage("Explain the payment terms")).isEqualTo("en");
		assertThat(AgentQuerySupport.documentQueryLanguage("支払条件を説明して")).isEqualTo("ja");
		assertThat(AgentQuerySupport.documentQueryLanguage("契約期間")).isEqualTo("ja");
		assertThat(AgentQuerySupport.documentQueryLanguage("1234")).isEqualTo("unknown");
		assertThat(AgentQuerySupport.documentQueryLanguage("REQ-LB-007 내용을 알려줘")).isNull();
		assertThat(AgentQuerySupport.documentQueryLanguage("contract.pdf 내용을 알려줘")).isNull();
	}

	@Test
	void excludesGenericProjectFramingFromAnswerabilityKeywords() {
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze(
				"업로드된 프로젝트 문서 기준으로 비밀번호 최소 길이를 알려줘",
				"ko-KR"
		);

		assertThat(analysis.keywords()).contains("업로드된", "프로젝트", "비밀번호");
		assertThat(analysis.rankingKeywords())
				.doesNotContain("업로드된", "프로젝트")
				.contains("비밀번호");
	}

	@Test
	void extractsJapaneseContentTermsWithoutSplittingKaraAndMadeIntoCharacters() {
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze(
				"利用者が指定された入場時間までにチェックインしなかった場合",
				"ja-JP"
		);

		assertThat(analysis.keywords())
				.containsExactly("利用者", "指定", "入場時間", "チェックイン", "場合")
				.doesNotContain("チェックインしな", "った場合");
	}

	@Test
	void removesEnglishDocumentFramingBeforeSelectingRetrievalKeywords() {
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze(
				"Based on the uploaded documents, what happens if a user does not check in by the assigned entry time?",
				"en-US"
		);

		assertThat(analysis.keywords())
				.contains("user", "check", "assigned", "entry")
				.doesNotContain("based", "on", "the", "uploaded", "documents", "happens");
		assertThat(analysis.normalizedQuery()).doesNotContain("uploaded", "documents", "happens");
	}

	@Test
	void recognizesUserAccountQuestionsThatMustNotFallbackToRoomDocuments() {
		assertThat(AgentQuerySupport.isUserAccountQuestion("내 ID가 뭐야")).isTrue();
		assertThat(AgentQuerySupport.isUserAccountQuestion("Who am I?")).isTrue();
		assertThat(AgentQuerySupport.isUserAccountQuestion("座席予約はどうなりますか")).isFalse();
	}

	@Test
	void stripsDocumentFramingWithoutBreakingEnglishPhrasalVerbForTranslation() {
		String query = AgentQuerySupport.semanticSearchQuery(
				"Based on the uploaded documents, what happens if a user does not check in by the assigned entry time?"
		);

		assertThat(query)
				.startsWith("what happens")
				.contains("does not check in")
				.doesNotContain("uploaded documents");
	}

	@Test
	void routesBroadDocumentQuestionsByRequestedOperation() {
		AgentSearchQueryAnalysis overview = AgentQuerySupport.analyze(
				"/bubli 의료예약_진료문진 해당 파일에 어떤 중요한 내용들이 있어?",
				"ko-KR"
		);
		AgentSearchQueryAnalysis checklist = AgentQuerySupport.analyze(
				"/bubli 의료예약_진료문진 해당 파일에 확인해야할 내용은 뭐야?",
				"ko-KR"
		);
		AgentSearchQueryAnalysis roleAnalysis = AgentQuerySupport.analyze(
				"/bubli 교육*LMS*과제수강을 바탕으로 백엔드 개발자로써 어떤 부분을 중점적으로 봐야해?",
				"ko-KR"
		);

		assertThat(overview.intent()).isEqualTo(ProjectRoomQueryIntent.DOCUMENT_OVERVIEW);
		assertThat(checklist.intent()).isEqualTo(ProjectRoomQueryIntent.REVIEW_CHECKLIST);
		assertThat(roleAnalysis.intent()).isEqualTo(ProjectRoomQueryIntent.ROLE_BASED_ANALYSIS);
		assertThat(roleAnalysis.perspective()).isEqualTo("BACKEND_DEVELOPER");
		assertThat(roleAnalysis.normalizedQuery()).contains("권한", "상태 전이", "API", "비기능 요구사항");
		assertThat(overview.normalizedQuery()).doesNotContain("의료예약", "진료문진");
		assertThat(checklist.normalizedQuery()).doesNotContain("의료예약", "진료문진");
		assertThat(roleAnalysis.normalizedQuery()).doesNotContain("교육", "LMS", "과제수강");
		assertThat(roleAnalysis.titleTokens()).contains("교육", "lms", "과제수강을");
	}

	@Test
	void leavesUnrecognizedConcreteQuestionForSemanticRouting() {
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze(
				"의료예약 문서에서 환자가 예약 전에 무엇을 입력해야 하나?",
				"ko-KR"
		);

		assertThat(analysis.intent()).isEqualTo(ProjectRoomQueryIntent.GENERAL_DOCUMENT_QA);
		assertThat(analysis.perspective()).isBlank();
	}
}
