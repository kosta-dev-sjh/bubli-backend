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
	private static final double ANSWERABILITY_MIN_SCORE = 0.52D;
	private static final double STRONG_SEMANTIC_ORIGINAL_SCORE = 0.88D;
	private static final double SEMANTIC_ONLY_ORIGINAL_SCORE = 0.78D;
	private static final double JAPANESE_SEMANTIC_ONLY_ORIGINAL_SCORE = 0.72D;
	private static final double ADJACENT_CHUNK_DUPLICATE_SIMILARITY = 0.55D;
	private static final int RRF_K = 60;
	private static final double RRF_WEIGHT = 1.8D;

	private final ResourceSearchMetricsPublicService resourceSearchMetrics;

	ProjectRoomDocumentFusionResult fuse(
			AgentSearchQueryAnalysis analysis,
			List<ProjectRoomDocumentCandidate> candidates,
			int limit,
			AgentCommandModeValue mode
	) {
		if (candidates == null || candidates.isEmpty()) {
			resourceSearchMetrics.recordFusion("room", 0, 0, false, "NONE");
			return new ProjectRoomDocumentFusionResult(
					List.of(),
					List.of(),
					0,
					false,
					"NONE",
					0.0D,
					"NO_DOCUMENT_CANDIDATE"
			);
		}
		Map<UUID, ProjectRoomDocumentCandidate> byEmbeddingId = new LinkedHashMap<>();
		for (ProjectRoomDocumentCandidate candidate : applyReciprocalRankFeature(candidates)) {
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
		int selectedCandidateCountBeforeGate = selected.size();
		AnswerabilityDecision answerability = decideAnswerability(analysis, selected, mode);
		boolean grounded = !selected.isEmpty() && answerability.answerable();
		if (!grounded) {
			selected = List.of();
		}
		resourceSearchMetrics.recordFusion(
				"room",
				candidates.size(),
				selected.size(),
				grounded,
				selected.isEmpty() ? "NONE" : selected.getFirst().retrievalMode()
		);
		return new ProjectRoomDocumentFusionResult(selected, ranked, selectedCandidateCountBeforeGate, grounded, selected.isEmpty()
				? "NONE"
				: selected.getFirst().retrievalMode(), answerability.score(), answerability.reason());
	}

	private AnswerabilityDecision decideAnswerability(
			AgentSearchQueryAnalysis analysis,
			List<ProjectRoomDocumentCandidate> selected,
			AgentCommandModeValue mode
	) {
		if (selected.isEmpty()) {
			return new AnswerabilityDecision(false, 0.0D, "NO_SELECTED_DOCUMENT_CANDIDATE");
		}
		if (mode == AgentCommandModeValue.SUGGEST) {
			return new AnswerabilityDecision(true, 1.0D, "SUGGEST_MODE");
		}
		ProjectRoomDocumentCandidate top = selected.getFirst();
		if (hasHardMatch(top, analysis)) {
			return new AnswerabilityDecision(true, 1.0D, "HARD_MATCH");
		}
		if (isStrongSemanticEvidence(top, selected)) {
			return new AnswerabilityDecision(true, 0.90D, "STRONG_SEMANTIC_MATCH");
		}
		if (isStrongScopedEvidence(top, selected)) {
			return new AnswerabilityDecision(true, 0.82D, "STRONG_SCOPED_RETRIEVAL_MATCH");
		}
		if (isConfidentSemanticOnlyEvidence(analysis, top)) {
			return new AnswerabilityDecision(true, 0.74D, "CONFIDENT_SEMANTIC_ONLY_MATCH");
		}
		List<String> rankingKeywords = analysis.rankingKeywords();
		double keywordCoverage = keywordCoverage(rankingKeywords, selected);
		double normalizedFusionScore = Math.min(1.0D, top.fusionScore() / 1.2D);
		double semanticSignal = top.retrievalMode().contains("SEMANTIC") && top.originalScore() >= 0.68D ? 0.15D : 0.0D;
		double evidenceSignal = selected.size() >= 2 ? 0.05D : 0.0D;
		double score = (normalizedFusionScore * 0.45D)
				+ (keywordCoverage * 0.35D)
				+ semanticSignal
				+ evidenceSignal;
		boolean answerable = score >= ANSWERABILITY_MIN_SCORE
				&& (keywordCoverage > 0.0D || semanticSignal > 0.0D);
		return new AnswerabilityDecision(
				answerable,
				Math.round(score * 1000.0D) / 1000.0D,
				answerable ? "ANSWERABILITY_GATE_PASSED" : "LOW_ANSWERABILITY"
		);
	}

	private boolean isConfidentSemanticOnlyEvidence(
			AgentSearchQueryAnalysis analysis,
			ProjectRoomDocumentCandidate top
	) {
		if (!top.retrievalMode().contains("SEMANTIC")) {
			return false;
		}
		double requiredOriginalScore = AgentQuerySupport.isJapaneseLocale(analysis.locale())
				? JAPANESE_SEMANTIC_ONLY_ORIGINAL_SCORE
				: SEMANTIC_ONLY_ORIGINAL_SCORE;
		return top.originalScore() >= requiredOriginalScore
				&& top.fusionScore() >= ANSWER_MIN_FUSION_SCORE;
	}

	private boolean isStrongSemanticEvidence(
			ProjectRoomDocumentCandidate top,
			List<ProjectRoomDocumentCandidate> selected
	) {
		return top.retrievalMode().contains("SEMANTIC")
				&& top.originalScore() >= STRONG_SEMANTIC_ORIGINAL_SCORE
				&& (selected.size() >= 2 || top.fusionScore() >= STRONG_SEMANTIC_ORIGINAL_SCORE);
	}

	private boolean isStrongScopedEvidence(
			ProjectRoomDocumentCandidate top,
			List<ProjectRoomDocumentCandidate> selected
	) {
		return top.retrievalMode().contains("TITLE_SCOPED")
				&& top.originalScore() >= 0.58D
				&& (top.matchedKeywords().size() >= 2 || selected.size() >= 2 || top.retrievalMode().contains("SEMANTIC")
						|| (top.retrievalMode().contains("REPRESENTATIVE") && top.originalScore() >= 0.90D));
	}

	private List<ProjectRoomDocumentCandidate> applyReciprocalRankFeature(
			List<ProjectRoomDocumentCandidate> candidates
	) {
		Map<UUID, Double> rrfByEmbeddingId = new LinkedHashMap<>();
		Map<String, List<ProjectRoomDocumentCandidate>> byMode = new LinkedHashMap<>();
		for (ProjectRoomDocumentCandidate candidate : candidates) {
			if (candidate == null || candidate.hit() == null || candidate.hit().embeddingId() == null) {
				continue;
			}
			byMode.computeIfAbsent(candidate.retrievalMode(), ignored -> new ArrayList<>()).add(candidate);
		}
		for (List<ProjectRoomDocumentCandidate> modeCandidates : byMode.values()) {
			List<ProjectRoomDocumentCandidate> ranked = modeCandidates.stream()
					.sorted(Comparator.comparingDouble(ProjectRoomDocumentCandidate::originalScore).reversed()
							.thenComparing(candidate -> candidate.hit().chunkIndex()))
					.toList();
			for (int index = 0; index < ranked.size(); index++) {
				UUID embeddingId = ranked.get(index).hit().embeddingId();
				double contribution = RRF_WEIGHT / (RRF_K + index + 1);
				rrfByEmbeddingId.merge(embeddingId, contribution, Double::sum);
			}
		}
		return candidates.stream()
				.map(candidate -> {
					if (candidate == null || candidate.hit() == null || candidate.hit().embeddingId() == null) {
						return candidate;
					}
					return candidate.withReciprocalRankScore(
							Math.round(rrfByEmbeddingId.getOrDefault(candidate.hit().embeddingId(), 0.0D) * 1000.0D)
									/ 1000.0D
					);
				})
				.toList();
	}

	private double keywordCoverage(
			List<String> rankingKeywords,
			List<ProjectRoomDocumentCandidate> selected
	) {
		if (rankingKeywords.isEmpty()) {
			return 0.0D;
		}
		Set<String> matched = new HashSet<>();
		for (ProjectRoomDocumentCandidate candidate : selected) {
			for (String keyword : candidate.matchedKeywords()) {
				String compactKeyword = AgentQuerySupport.compactResourceText(keyword);
				if (!compactKeyword.isBlank()) {
					matched.add(compactKeyword);
				}
			}
		}
		long expected = rankingKeywords.stream()
				.map(AgentQuerySupport::compactResourceText)
				.filter(keyword -> !keyword.isBlank())
				.distinct()
				.count();
		if (expected == 0L) {
			return 0.0D;
		}
		return (double) matched.size() / expected;
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
				if (isNearDuplicate(candidate, selected) || isRedundantAdjacentChunk(candidate, selected)) {
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
				+ (isRedundantAdjacentChunk(candidate, selected) ? ADJACENT_CHUNK_PENALTY : 0.0D);
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

	private boolean isRedundantAdjacentChunk(
			ProjectRoomDocumentCandidate candidate,
			List<ProjectRoomDocumentCandidate> selected
	) {
		return isAdjacentChunk(candidate, selected)
				&& maxTextSimilarity(candidate, selected) >= ADJACENT_CHUNK_DUPLICATE_SIMILARITY;
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
			int selectedCandidateCountBeforeGate,
			boolean grounded,
			String primaryRetrievalMode,
			double answerabilityScore,
			String answerabilityReason
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

	private record AnswerabilityDecision(
			boolean answerable,
			double score,
			String reason
	) {
	}
}
