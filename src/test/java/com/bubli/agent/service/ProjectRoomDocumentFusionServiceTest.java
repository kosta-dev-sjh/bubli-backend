package com.bubli.agent.service;

import com.bubli.resource.dto.ResourceSearchHit;
import com.bubli.resource.service.ResourceSearchMetricsPublicService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProjectRoomDocumentFusionServiceTest {

	@Test
	void exactRequirementKeywordBeatsHigherSemanticScore() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze("REQ-LB-004 내용 알려줘", "ko-KR");
		UUID semanticResourceId = UUID.randomUUID();
		UUID keywordResourceId = UUID.randomUUID();

		var result = fusionService.fuse(
				analysis,
				List.of(
						ProjectRoomDocumentCandidate.of(hit(semanticResourceId, "general project schedule text", 0.91D),
								"SEMANTIC", analysis, false),
						ProjectRoomDocumentCandidate.of(hit(keywordResourceId, "REQ-LB-004 프로젝트 일정 관리 기능", 1.0D),
								"KEYWORD", analysis, false)
				),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.grounded()).isTrue();
		assertThat(result.hits().getFirst().resourceId()).isEqualTo(keywordResourceId);
		assertThat(result.selected().getFirst().matchReason()).contains("REQUIREMENT_ID_MATCH");
		verify(metrics).recordFusion(eq("room"), eq(2), eq(2), eq(true), eq("KEYWORD"));
	}

	@Test
	void limitsChunksPerResourceToPreventDominance() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze("project schedule summary", "en-US");
		UUID dominantResourceId = UUID.randomUUID();
		UUID otherResourceId = UUID.randomUUID();

		var result = fusionService.fuse(
				analysis,
				List.of(
						ProjectRoomDocumentCandidate.of(hit(dominantResourceId, "project schedule alpha", 0.95D),
								"SEMANTIC", analysis, false),
						ProjectRoomDocumentCandidate.of(hit(dominantResourceId, "project schedule beta", 0.94D, 2),
								"SEMANTIC", analysis, false),
						ProjectRoomDocumentCandidate.of(hit(dominantResourceId, "project schedule gamma", 0.93D, 4),
								"SEMANTIC", analysis, false),
						ProjectRoomDocumentCandidate.of(hit(otherResourceId, "project schedule delta", 0.90D),
								"SEMANTIC", analysis, false)
				),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.hits())
				.extracting(ResourceSearchHit::resourceId)
				.containsExactly(dominantResourceId, dominantResourceId, otherResourceId);
	}

	@Test
	void reciprocalRankInterleavesDocumentsBeforeMoreChunksFromDominantDocument() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze("project schedule summary", "en-US");
		UUID dominantResourceId = UUID.randomUUID();
		UUID otherResourceId = UUID.randomUUID();
		ProjectRoomDocumentCandidate otherDocumentCandidate = ProjectRoomDocumentCandidate.of(
				hit(otherResourceId, "project schedule from the other document", 0.90D),
				"SEMANTIC",
				analysis,
				false
		);

		var result = fusionService.fuse(
				analysis,
				List.of(
						ProjectRoomDocumentCandidate.of(
								hit(dominantResourceId, "project schedule alpha", 0.95D, 0),
								"SEMANTIC", analysis, false),
						ProjectRoomDocumentCandidate.of(
								hit(dominantResourceId, "project schedule beta", 0.94D, 2),
								"SEMANTIC", analysis, false),
						ProjectRoomDocumentCandidate.of(
								hit(dominantResourceId, "project schedule gamma", 0.93D, 4),
								"SEMANTIC", analysis, false),
						otherDocumentCandidate
				),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.ranked().stream()
				.filter(candidate -> candidate.hit().resourceId().equals(otherResourceId))
				.findFirst()
				.orElseThrow()
				.reciprocalRankScore()).isEqualTo(0.029D);
	}

	@Test
	void finalLimitIsAppliedAfterCandidateOverfetch() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze("project schedule summary", "en-US");
		List<ProjectRoomDocumentCandidate> candidates = java.util.stream.IntStream.range(0, 12)
				.mapToObj(index -> ProjectRoomDocumentCandidate.of(
						hit(UUID.randomUUID(), "project schedule evidence " + UUID.randomUUID(),
								0.95D - index * 0.01D),
						"SEMANTIC",
						analysis,
						false
				))
				.toList();

		var result = fusionService.fuse(
				analysis,
				candidates,
				3,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.selected()).hasSize(3);
		assertThat(result.ranked()).hasSize(12);
	}

	@Test
	void skipsAdjacentChunksBeforeLowerScoredDiverseEvidence() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze("project schedule summary", "en-US");
		UUID resourceId = UUID.randomUUID();
		UUID otherResourceId = UUID.randomUUID();

		var result = fusionService.fuse(
				analysis,
				List.of(
						ProjectRoomDocumentCandidate.of(hit(resourceId, "project schedule alpha milestone", 0.95D, 3),
								"SEMANTIC", analysis, false),
						ProjectRoomDocumentCandidate.of(hit(resourceId, "project schedule alpha milestone details", 0.94D, 4),
								"SEMANTIC", analysis, false),
						ProjectRoomDocumentCandidate.of(hit(otherResourceId, "project budget approval timeline", 0.88D, 0),
								"SEMANTIC", analysis, false)
				),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.hits())
				.extracting(ResourceSearchHit::chunkIndex)
				.containsExactly(3, 0);
	}

	@Test
	void skipsNearDuplicateChunkTextAcrossResources() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze("프로젝트 일정 관리", "ko-KR");
		UUID firstResourceId = UUID.randomUUID();
		UUID duplicateResourceId = UUID.randomUUID();
		UUID diverseResourceId = UUID.randomUUID();

		var result = fusionService.fuse(
				analysis,
				List.of(
						ProjectRoomDocumentCandidate.of(hit(firstResourceId, "프로젝트 일정 관리 기능은 달력과 진행률을 제공한다.", 0.95D),
								"SEMANTIC", analysis, false),
						ProjectRoomDocumentCandidate.of(hit(duplicateResourceId, "프로젝트 일정 관리 기능은 달력과 진행률을 제공한다.", 0.94D),
								"SEMANTIC", analysis, false),
						ProjectRoomDocumentCandidate.of(hit(diverseResourceId, "프로젝트 산출물 검토와 승인 흐름을 관리한다.", 0.88D),
								"SEMANTIC", analysis, false)
				),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.hits())
				.extracting(ResourceSearchHit::resourceId)
				.containsExactly(firstResourceId, diverseResourceId);
	}

	@Test
	void lowConfidenceCandidatesAreNotGrounded() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze("unrelated question", "en-US");

		var result = fusionService.fuse(
				analysis,
				List.of(ProjectRoomDocumentCandidate.of(hit(UUID.randomUUID(), "weak unrelated text", 0.20D),
						"SEMANTIC", analysis, false)),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.grounded()).isFalse();
		assertThat(result.hits()).isEmpty();
		verify(metrics).recordFusion(eq("room"), eq(1), eq(0), eq(false), eq("NONE"));
	}

	@Test
	void genericDocumentKeywordsAloneAreNotAnswerable() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze("show document file material", "en-US");

		var result = fusionService.fuse(
				analysis,
				List.of(ProjectRoomDocumentCandidate.of(
						hit(UUID.randomUUID(), "document file material overview", 0.95D),
						"KEYWORD",
						analysis,
						false
				)),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.grounded()).isFalse();
		assertThat(result.answerabilityReason()).isEqualTo("LOW_ANSWERABILITY");
	}

	@Test
	void genericSingleTokenTitleScopedMatchIsNotAnswerable() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze("roadmap payment deadline", "en-US");
		ProjectRoomDocumentCandidate candidate = ProjectRoomDocumentCandidate.of(
				hit(UUID.randomUUID(), "roadmap notes unrelated to the requested policy", 0.70D),
				"TITLE_SCOPED_SEMANTIC",
				analysis,
				true
		);

		var result = fusionService.fuse(
				analysis,
				List.of(candidate),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(candidate.matchedKeywords()).containsExactly("roadmap");
		assertThat(candidate.fusionScore()).isLessThan(0.90D);
		assertThat(result.grounded()).isFalse();
		assertThat(result.answerabilityReason()).isEqualTo("LOW_TITLE_SCOPED_ANSWERABILITY");
	}

	@Test
	void uploadedProjectFramingDoesNotMakeUnrelatedPolicyAnswerable() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze(
				"업로드된 프로젝트 문서 기준으로 비밀번호 최소 길이를 알려줘",
				"ko-KR"
		);
		ProjectRoomDocumentCandidate candidate = ProjectRoomDocumentCandidate.of(
				hit(UUID.randomUUID(), "프로젝트는 이메일과 비밀번호 기반 회원가입을 제공한다.", 0.85D),
				"KEYWORD",
				analysis,
				true
		);

		var result = fusionService.fuse(
				analysis,
				List.of(candidate),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(candidate.matchedKeywords()).containsExactly("비밀번호");
		assertThat(result.grounded()).isFalse();
		assertThat(result.answerabilityReason()).isEqualTo("LOW_ANSWERABILITY");
	}

	@Test
	void excludedScopeKeywordCannotBeCombinedWithGenericEvidenceFromOtherDocuments() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze("환불 처리 마감일", "ko-KR");

		var result = fusionService.fuse(
				analysis,
				List.of(
						ProjectRoomDocumentCandidate.of(
								hit(UUID.randomUUID(), "제외 범위: 결제와 환불", 1.0D, 0),
								"KEYWORD",
								analysis,
								false
						),
						ProjectRoomDocumentCandidate.of(
								hit(UUID.randomUUID(), "자료 처리 상태와 작업 마감일을 관리한다", 1.0D, 0),
								"KEYWORD",
								analysis,
								false
						)
				),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.grounded()).isFalse();
		assertThat(result.answerabilityReason()).isEqualTo("EXCLUDED_SCOPE_ONLY_EVIDENCE");
	}

	@Test
	void exclusionHeadingDoesNotHideSupportedEvidenceLaterInTheChunk() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze("주문 취소 승인 재고", "ko-KR");
		String chunkText = "제외 범위: 외부 결제 연동. " + "가".repeat(200)
				+ " 주문 취소 승인 시 재고를 즉시 복원한다.";

		var result = fusionService.fuse(
				analysis,
				List.of(ProjectRoomDocumentCandidate.of(
						hit(UUID.randomUUID(), chunkText, 1.0D, 0),
						"KEYWORD",
						analysis,
						false
				)),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.grounded()).isTrue();
		assertThat(result.answerabilityReason()).isNotEqualTo("EXCLUDED_SCOPE_ONLY_EVIDENCE");
	}

	@Test
	void strongBodyAgreementMakesTitleScopedCandidateAnswerable() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze("payment deadline policy", "en-US");
		UUID resourceId = UUID.randomUUID();

		var result = fusionService.fuse(
				analysis,
				List.of(ProjectRoomDocumentCandidate.of(
						hit(resourceId, "payment deadline policy is 30 days after approval", 0.60D),
						"TITLE_SCOPED_SEMANTIC",
						analysis,
						true
				)),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.grounded()).isTrue();
		assertThat(result.hits()).extracting(ResourceSearchHit::resourceId).containsExactly(resourceId);
		assertThat(result.answerabilityReason()).isEqualTo("STRONG_SCOPED_BODY_MATCH");
	}

	@Test
	void informationKeywordCoverageMakesKeywordCandidateAnswerable() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze("payment deadline policy", "en-US");
		UUID resourceId = UUID.randomUUID();

		var result = fusionService.fuse(
				analysis,
				List.of(ProjectRoomDocumentCandidate.of(
						hit(resourceId, "payment deadline policy is defined as 30 days after approval", 0.75D),
						"KEYWORD",
						analysis,
						false
				)),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.grounded()).isTrue();
		assertThat(result.hits()).extracting(ResourceSearchHit::resourceId).containsExactly(resourceId);
		assertThat(result.answerabilityScore()).isGreaterThanOrEqualTo(0.52D);
	}

	@Test
	void acceptsConfidentJapaneseSemanticEvidenceWithoutKeywordCoverage() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze(
				"安全在庫以下の商品を確認する方法を教えてください",
				"ja-JP"
		);

		var result = fusionService.fuse(
				analysis,
				List.of(ProjectRoomDocumentCandidate.of(
						hit(UUID.randomUUID(), "inventory threshold behaviour is described here", 0.74D),
						"SEMANTIC",
						analysis,
						false
				)),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.grounded()).isTrue();
		assertThat(result.answerabilityReason()).isEqualTo("CONFIDENT_SEMANTIC_ONLY_MATCH");
	}

	@Test
	void acceptsJapaneseParaphraseWhenTwoOfFiveTermsAgreeAcrossMultipleChunks() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		List<String> keywords = List.of("患者", "予約", "確定", "入力", "問診");
		AgentSearchQueryAnalysis analysis = new AgentSearchQueryAnalysis(
				"患者予約の事前問診",
				"ja-JP",
				keywords,
				List.of(),
				List.of(),
				List.of(),
				List.of(keywords)
		);
		UUID resourceId = UUID.randomUUID();

		var result = fusionService.fuse(
				analysis,
				List.of(
						ProjectRoomDocumentCandidate.of(hit(resourceId,
								"患者予約の事前問診フォームでは氏名と連絡先を記録する。", 0.65D, 0),
								"KEYWORD", analysis, false),
						ProjectRoomDocumentCandidate.of(hit(resourceId,
								"患者予約時には症状、服薬、アレルギー情報を登録する。", 0.64D, 2),
								"KEYWORD", analysis, false)
				),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(analysis.rankingKeywords()).hasSize(5);
		assertThat(result.grounded()).isTrue();
		assertThat(result.answerabilityReason()).isEqualTo("ANSWERABILITY_GATE_PASSED");
	}

	@Test
	void evaluatesCrossLanguageKeywordCoveragePerLanguageInsteadOfCombinedDenominator() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		List<String> korean = List.of("사용자", "지정", "입장", "시간", "체크인");
		List<String> japanese = List.of("利用者", "指定", "入場時間", "チェックイン", "場合");
		List<String> merged = new java.util.ArrayList<>(korean);
		merged.addAll(japanese);
		AgentSearchQueryAnalysis analysis = new AgentSearchQueryAnalysis(
				"user check in deadline",
				"en-US",
				merged,
				List.of(),
				List.of(),
				List.of(),
				List.of(korean, japanese)
		);

		var result = fusionService.fuse(
				analysis,
				List.of(ProjectRoomDocumentCandidate.of(
						hit(UUID.randomUUID(), "利用者は指定時刻までにチェックインする。", 0.65D),
						"KEYWORD",
						analysis,
						false
				)),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.grounded()).isTrue();
		assertThat(result.answerabilityReason()).isEqualTo("ANSWERABILITY_GATE_PASSED");
	}

	@Test
	void genericScheduleAndPeriodMatchesDoNotPassLexicalAnswerabilityGate() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		List<String> keywords = List.of("backup", "schedule", "retention", "period");
		AgentSearchQueryAnalysis analysis = new AgentSearchQueryAnalysis(
				"backup schedule and retention period",
				"en-US",
				keywords,
				List.of(),
				List.of(),
				List.of(),
				List.of(keywords)
		);

		var result = fusionService.fuse(
				analysis,
				List.of(ProjectRoomDocumentCandidate.of(
						hit(UUID.randomUUID(), "The project schedule covers a six month period.", 0.85D),
						"KEYWORD",
						analysis,
						false
				)),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.grounded()).isFalse();
		assertThat(result.answerabilityReason()).isEqualTo("LOW_ANSWERABILITY");
	}

	@Test
	void keepsComplementaryAdjacentChunks() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze("payment approval exception workflow", "en-US");
		UUID resourceId = UUID.randomUUID();

		var result = fusionService.fuse(
				analysis,
				List.of(
						ProjectRoomDocumentCandidate.of(hit(resourceId,
								"payment approval begins only after invoice validation", 0.95D, 3), "SEMANTIC", analysis, false),
						ProjectRoomDocumentCandidate.of(hit(resourceId,
								"exception workflow sends a finance-owner notification", 0.94D, 4), "SEMANTIC", analysis, false)
				),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.hits()).extracting(ResourceSearchHit::chunkIndex).containsExactly(3, 4);
	}

	@Test
	void explicitlySelectedDocumentAllowsGroundedReviewChecklistWithPartialStatus() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze(
				"이 문서에서 확인해야 할 내용을 정리해줘",
				"ko-KR"
		).withScopeConfidence(DocumentScopeConfidence.EXPLICIT);
		UUID resourceId = UUID.randomUUID();

		var result = fusionService.fuse(
				analysis,
				List.of(
						ProjectRoomDocumentCandidate.of(hit(resourceId,
								"환자는 예약 전에 증상과 알레르기 정보를 입력한다.", 0.50D, 0),
								"REPRESENTATIVE", analysis, true),
						ProjectRoomDocumentCandidate.of(hit(resourceId,
								"의사는 문진 내용을 확인하고 진료 가능 시간을 확정한다.", 0.49D, 2),
								"REPRESENTATIVE", analysis, true)
				),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.grounded()).isTrue();
		assertThat(result.selected()).hasSize(2);
		assertThat(result.answerabilityStatus()).isEqualTo(ProjectRoomAnswerabilityStatus.PARTIALLY_ANSWERABLE);
		assertThat(result.answerabilityReason()).isEqualTo("GROUNDED_DOCUMENT_SYNTHESIS");
	}

	@Test
	void strongTitleScopeAllowsRoleBasedAnalysisWithoutVerbatimPerspectiveTerms() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze(
				"교육 LMS 과제수강을 바탕으로 백엔드 개발자가 중점적으로 볼 부분은?",
				"ko-KR"
		).withScopeConfidence(DocumentScopeConfidence.STRONG_TITLE);

		var result = fusionService.fuse(
				analysis,
				List.of(ProjectRoomDocumentCandidate.of(
						hit(UUID.randomUUID(), "학생은 과제 파일을 제출하고 강사는 점수와 피드백을 입력한다.", 0.50D),
						"REPRESENTATIVE", analysis, true
				)),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.grounded()).isTrue();
		assertThat(result.answerabilityStatus()).isEqualTo(ProjectRoomAnswerabilityStatus.PARTIALLY_ANSWERABLE);
	}

	@Test
	void titleWordsDoNotHideStrongBodyAgreementForSpecificFactQuestion() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze(
				"library requirements reserved user misses check in deadline",
				"en-US"
		).withScopeConfidence(DocumentScopeConfidence.STRONG_TITLE);
		UUID resourceId = UUID.randomUUID();
		ResourceSearchHit hit = new ResourceSearchHit(
				UUID.randomUUID(), resourceId, 1,
				"A reserved user who misses the check in deadline has the reservation cancelled.",
				1, 1, 3, 0, 100, "library_requirements.pdf", "{}", 0.65D
		);

		var result = fusionService.fuse(
				analysis,
				List.of(ProjectRoomDocumentCandidate.of(hit, "TITLE_SCOPED_KEYWORD", analysis, true)),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.grounded()).isTrue();
		assertThat(result.answerabilityReason()).isEqualTo("STRONG_SCOPED_BODY_MATCH");
	}

	@Test
	void titleAndTopicWordsAloneDoNotAnswerMissingSpecificFact() {
		ResourceSearchMetricsPublicService metrics = mock(ResourceSearchMetricsPublicService.class);
		ProjectRoomDocumentFusionService fusionService = new ProjectRoomDocumentFusionService(metrics);
		AgentSearchQueryAnalysis analysis = AgentQuerySupport.analyze(
				"project room requirements freelance settlement fee formula",
				"en-US"
		).withScopeConfidence(DocumentScopeConfidence.STRONG_TITLE);
		UUID resourceId = UUID.randomUUID();
		ResourceSearchHit overview = new ResourceSearchHit(
				UUID.randomUUID(), resourceId, 0,
				"This project room is designed for freelance workers.",
				1, 1, 3, 0, 100, "project_room_requirements.pdf", "{}", 0.85D
		);
		ResourceSearchHit excluded = new ResourceSearchHit(
				UUID.randomUUID(), resourceId, 1,
				"Out of scope: settlement accounting.",
				2, 1, 3, 0, 100, "project_room_requirements.pdf", "{}", 0.85D
		);

		var result = fusionService.fuse(
				analysis,
				List.of(
						ProjectRoomDocumentCandidate.of(overview, "TITLE_SCOPED_KEYWORD", analysis, true),
						ProjectRoomDocumentCandidate.of(excluded, "TITLE_SCOPED_KEYWORD", analysis, true)
				),
				5,
				ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
		);

		assertThat(result.grounded()).isFalse();
		assertThat(result.answerabilityReason()).isEqualTo("LOW_TITLE_SCOPED_ANSWERABILITY");
	}

	private ResourceSearchHit hit(UUID resourceId, String chunkText, double score) {
		return hit(resourceId, chunkText, score, 0);
	}

	private ResourceSearchHit hit(UUID resourceId, String chunkText, double score, int chunkIndex) {
		return new ResourceSearchHit(
				UUID.randomUUID(),
				resourceId,
				chunkIndex,
				chunkText,
				1,
				1,
				3,
				0,
				100,
				"requirements.pdf",
				"{}",
				score
		);
	}
}
