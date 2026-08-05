package com.bubli.agent.service;

import com.bubli.resource.dto.ResourceSearchHit;
import com.bubli.resource.service.ResourceSearchMetricsPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ProjectRoomDocumentFusionService {

	private static final int PER_RESOURCE_LIMIT = 2;
	private static final int ADJACENT_CHUNK_WINDOW = 1;
	private static final double NEAR_DUPLICATE_TEXT_SIMILARITY = 0.82D;
	private static final double TEXT_REDUNDANCY_PENALTY = 0.35D;
	private static final double ADJACENT_CHUNK_PENALTY = 0.25D;
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
		List<ProjectRoomDocumentCandidate> selected = selectDiverseCandidates(ranked, Math.max(1, limit));
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

	private List<ProjectRoomDocumentCandidate> selectDiverseCandidates(
			List<ProjectRoomDocumentCandidate> ranked,
			int limit
	) {
		List<ProjectRoomDocumentCandidate> selected = new ArrayList<>();
		Map<UUID, Integer> countByResource = new LinkedHashMap<>();
		List<ProjectRoomDocumentCandidate> remaining = new ArrayList<>(ranked);
		while (selected.size() < limit && !remaining.isEmpty()) {
			ProjectRoomDocumentCandidate bestCandidate = null;
			double bestScore = Double.NEGATIVE_INFINITY;
			for (ProjectRoomDocumentCandidate candidate : remaining) {
				UUID resourceId = candidate.hit().resourceId();
				if (countByResource.getOrDefault(resourceId, 0) >= PER_RESOURCE_LIMIT) {
					continue;
				}
				if (isNearDuplicate(candidate, selected) || isAdjacentChunk(candidate, selected)) {
					continue;
				}
				double mmrScore = candidate.fusionScore() - redundancyPenalty(candidate, selected);
				if (mmrScore > bestScore) {
					bestScore = mmrScore;
					bestCandidate = candidate;
				}
			}
			if (bestCandidate == null) {
				break;
			}
			selected.add(bestCandidate);
			UUID resourceId = bestCandidate.hit().resourceId();
			countByResource.put(resourceId, countByResource.getOrDefault(resourceId, 0) + 1);
			remaining.remove(bestCandidate);
		}
		return selected;
	}

	private double redundancyPenalty(
			ProjectRoomDocumentCandidate candidate,
			List<ProjectRoomDocumentCandidate> selected
	) {
		if (selected.isEmpty()) {
			return 0.0D;
		}
		return (maxTextSimilarity(candidate, selected) * TEXT_REDUNDANCY_PENALTY)
				+ (isAdjacentChunk(candidate, selected) ? ADJACENT_CHUNK_PENALTY : 0.0D);
	}

	private boolean isAdjacentChunk(
			ProjectRoomDocumentCandidate candidate,
			List<ProjectRoomDocumentCandidate> selected
	) {
		return selected.stream()
				.anyMatch(selectedCandidate -> selectedCandidate.hit().resourceId().equals(candidate.hit().resourceId())
						&& Math.abs(selectedCandidate.hit().chunkIndex() - candidate.hit().chunkIndex())
						<= ADJACENT_CHUNK_WINDOW);
	}

	private boolean isNearDuplicate(
			ProjectRoomDocumentCandidate candidate,
			List<ProjectRoomDocumentCandidate> selected
	) {
		return maxTextSimilarity(candidate, selected) >= NEAR_DUPLICATE_TEXT_SIMILARITY;
	}

	private double maxTextSimilarity(
			ProjectRoomDocumentCandidate candidate,
			List<ProjectRoomDocumentCandidate> selected
	) {
		Set<String> candidateShingles = textShingles(candidate.hit().chunkText());
		if (candidateShingles.isEmpty()) {
			return 0.0D;
		}
		return selected.stream()
				.map(ProjectRoomDocumentCandidate::hit)
				.map(ResourceSearchHit::chunkText)
				.map(this::textShingles)
				.mapToDouble(selectedShingles -> jaccard(candidateShingles, selectedShingles))
				.max()
				.orElse(0.0D);
	}

	private Set<String> textShingles(String text) {
		String compact = AgentQuerySupport.compactResourceText(text).replace(" ", "");
		if (compact.isBlank()) {
			return Set.of();
		}
		if (compact.length() <= 5) {
			return Set.of(compact);
		}
		Set<String> shingles = new HashSet<>();
		for (int index = 0; index <= compact.length() - 5; index++) {
			shingles.add(compact.substring(index, index + 5));
		}
		return shingles;
	}

	private double jaccard(Set<String> first, Set<String> second) {
		if (first.isEmpty() || second.isEmpty()) {
			return 0.0D;
		}
		int intersection = 0;
		for (String token : first) {
			if (second.contains(token)) {
				intersection++;
			}
		}
		int union = first.size() + second.size() - intersection;
		return union == 0 ? 0.0D : (double) intersection / union;
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
