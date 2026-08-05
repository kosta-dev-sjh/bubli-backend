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
