package com.bubli.agent.service;

import com.bubli.resource.dto.ResourceSearchHit;
import com.bubli.resource.service.ResourceSearchMetricsPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ProjectRoomDocumentFusionService {

	private static final int PER_RESOURCE_LIMIT = 2;
	private static final double ANSWER_MIN_FUSION_SCORE = 0.62D;
	private static final double SUGGEST_MIN_FUSION_SCORE = 0.45D;

	private final ResourceSearchMetricsPublicService resourceSearchMetrics;

	ProjectRoomDocumentFusionResult fuse(
			AgentSearchQueryAnalysis analysis,
			List<ProjectRoomDocumentCandidate> candidates,
			int limit,
			AgentCommandModeValue mode
	) {
		if (candidates == null || candidates.isEmpty()) {
			resourceSearchMetrics.recordFusion("room", 0, 0, false, "NONE");
			return new ProjectRoomDocumentFusionResult(List.of(), List.of(), false, "NONE");
		}
		Map<UUID, ProjectRoomDocumentCandidate> byEmbeddingId = new LinkedHashMap<>();
		for (ProjectRoomDocumentCandidate candidate : candidates) {
			if (candidate == null || candidate.hit() == null || candidate.hit().embeddingId() == null) {
				continue;
			}
			byEmbeddingId.merge(candidate.hit().embeddingId(), candidate, ProjectRoomDocumentCandidate::merge);
		}
		double minScore = mode == AgentCommandModeValue.SUGGEST ? SUGGEST_MIN_FUSION_SCORE : ANSWER_MIN_FUSION_SCORE;
		List<ProjectRoomDocumentCandidate> ranked = byEmbeddingId.values().stream()
				.filter(candidate -> candidate.fusionScore() >= minScore || hasHardMatch(candidate, analysis))
				.sorted(Comparator.comparingDouble(ProjectRoomDocumentCandidate::fusionScore).reversed()
						.thenComparing(candidate -> candidate.hit().chunkIndex()))
				.toList();
		List<ProjectRoomDocumentCandidate> selected = limitPerResource(ranked, Math.max(1, limit));
		boolean grounded = !selected.isEmpty();
		resourceSearchMetrics.recordFusion(
				"room",
				candidates.size(),
				selected.size(),
				grounded,
				selected.isEmpty() ? "NONE" : selected.getFirst().retrievalMode()
		);
		return new ProjectRoomDocumentFusionResult(selected, ranked, grounded, selected.isEmpty()
				? "NONE"
				: selected.getFirst().retrievalMode());
	}

	private boolean hasHardMatch(ProjectRoomDocumentCandidate candidate, AgentSearchQueryAnalysis analysis) {
		if (analysis.hasPreciseIdentifier() && candidate.matchReason().contains("REQUIREMENT_ID_MATCH")) {
			return true;
		}
		return !analysis.quotedPhrases().isEmpty() && candidate.matchReason().contains("QUOTED_PHRASE_MATCH");
	}

	private List<ProjectRoomDocumentCandidate> limitPerResource(
			List<ProjectRoomDocumentCandidate> ranked,
			int limit
	) {
		List<ProjectRoomDocumentCandidate> selected = new ArrayList<>();
		Map<UUID, Integer> countByResource = new LinkedHashMap<>();
		for (ProjectRoomDocumentCandidate candidate : ranked) {
			UUID resourceId = candidate.hit().resourceId();
			int resourceCount = countByResource.getOrDefault(resourceId, 0);
			if (resourceCount >= PER_RESOURCE_LIMIT) {
				continue;
			}
			selected.add(candidate);
			countByResource.put(resourceId, resourceCount + 1);
			if (selected.size() >= limit) {
				break;
			}
		}
		return selected;
	}

	record ProjectRoomDocumentFusionResult(
			List<ProjectRoomDocumentCandidate> selected,
			List<ProjectRoomDocumentCandidate> ranked,
			boolean grounded,
			String primaryRetrievalMode
	) {

		List<ResourceSearchHit> hits() {
			return selected.stream()
					.map(ProjectRoomDocumentCandidate::hit)
					.toList();
		}
	}

	enum AgentCommandModeValue {
		ANSWER,
		SUGGEST
	}
}
