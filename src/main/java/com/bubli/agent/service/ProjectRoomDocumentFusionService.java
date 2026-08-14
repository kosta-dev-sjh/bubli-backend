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
	private static final double MIN_LEXICAL_ANSWERABILITY_COVERAGE = 0.50D;
	private static final double MIN_JAPANESE_LEXICAL_ANSWERABILITY_COVERAGE = 0.40D;
	private static final double STRONG_SEMANTIC_ORIGINAL_SCORE = 0.88D;
	private static final double SEMANTIC_ONLY_ORIGINAL_SCORE = 0.78D;
	private static final double JAPANESE_SEMANTIC_ONLY_ORIGINAL_SCORE = 0.72D;
	private static final int TITLE_SCOPED_MIN_MATCHED_KEYWORDS = 2;
	private static final double TITLE_SCOPED_MIN_KEYWORD_COVERAGE = 0.50D;
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
					"NO_DOCUMENT_CANDIDATE",
					ProjectRoomAnswerabilityStatus.NO_EVIDENCE
			);
		}
		Map<UUID, ProjectRoomDocumentCandidate> byEmbeddingId = new LinkedHashMap<>();
		for (ProjectRoomDocumentCandidate candidate : applyReciprocalRankFeature(candidates)) {
			if (candidate == null || candidate.hit() == null || candidate.hit().embeddingId() == null) {
				continue;
			}
			byEmbeddingId.merge(candidate.hit().embeddingId(), candidate, ProjectRoomDocumentCandidate::merge);
		}
		boolean trustedSynthesisScope = analysis.intent().allowsDocumentSynthesis()
				&& analysis.scopeConfidence().isConfident();
		double minScore = mode == AgentCommandModeValue.SUGGEST || trustedSynthesisScope
				? SUGGEST_MIN_FUSION_SCORE
				: ANSWER_MIN_FUSION_SCORE;
		List<ProjectRoomDocumentCandidate> ranked = byEmbeddingId.values().stream()
				.filter(candidate -> candidate.fusionScore() >= minScore || hasHardMatch(candidate, analysis))
				.sorted(Comparator.comparingDouble(ProjectRoomDocumentCandidate::fusionScore).reversed()
						.thenComparing(candidate -> candidate.hit().chunkIndex()))
				.toList();
		List<ProjectRoomDocumentCandidate> selected = selectDiverseCandidates(
				preferSpecificEvidenceCandidates(ranked),
				Math.max(1, limit),
				analysis
		);
		int selectedCandidateCountBeforeGate = selected.size();
		AnswerabilityDecision answerability = decideAnswerability(analysis, selected, mode);
		boolean grounded = !selected.isEmpty() && answerability.status().canAnswer();
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
				: selected.getFirst().retrievalMode(), answerability.score(), answerability.reason(), answerability.status());
	}

	private AnswerabilityDecision decideAnswerability(
			AgentSearchQueryAnalysis analysis,
			List<ProjectRoomDocumentCandidate> selected,
			AgentCommandModeValue mode
	) {
		if (selected.isEmpty()) {
			return new AnswerabilityDecision(false, 0.0D, "NO_SELECTED_DOCUMENT_CANDIDATE",
					ProjectRoomAnswerabilityStatus.NO_EVIDENCE);
		}
		if (mode == AgentCommandModeValue.SUGGEST) {
			return new AnswerabilityDecision(true, 1.0D, "SUGGEST_MODE",
					ProjectRoomAnswerabilityStatus.ANSWERABLE);
		}
		ProjectRoomDocumentCandidate top = selected.getFirst();
		if (hasHardMatch(top, analysis)) {
			return new AnswerabilityDecision(true, 1.0D, "HARD_MATCH",
					ProjectRoomAnswerabilityStatus.ANSWERABLE);
		}
		if (isDocumentOverviewRepresentative(analysis, top)) {
			return new AnswerabilityDecision(true, 0.85D, "DOCUMENT_OVERVIEW_REPRESENTATIVE",
					ProjectRoomAnswerabilityStatus.ANSWERABLE);
		}
		if (isStrongSemanticEvidence(top, selected)) {
			return new AnswerabilityDecision(true, 0.90D, "STRONG_SEMANTIC_MATCH",
					ProjectRoomAnswerabilityStatus.ANSWERABLE);
		}
		if (isConfidentSemanticOnlyEvidence(analysis, top)) {
			return new AnswerabilityDecision(true, 0.74D, "CONFIDENT_SEMANTIC_ONLY_MATCH",
					ProjectRoomAnswerabilityStatus.ANSWERABLE);
		}
		if (isStrongScopedEvidence(analysis, top, selected)) {
			return new AnswerabilityDecision(true, 0.82D, "STRONG_SCOPED_BODY_MATCH",
					ProjectRoomAnswerabilityStatus.ANSWERABLE);
		}
		if (hasExclusionOnlyAnchorEvidence(analysis, selected)) {
			return new AnswerabilityDecision(false, 0.0D, "EXCLUDED_SCOPE_ONLY_EVIDENCE",
					ProjectRoomAnswerabilityStatus.NO_EVIDENCE);
		}
		if (analysis.intent().allowsDocumentSynthesis() && analysis.scopeConfidence().isConfident()) {
			long resourceCount = selected.stream().map(candidate -> candidate.hit().resourceId()).distinct().count();
			if (analysis.intent() == ProjectRoomQueryIntent.DOCUMENT_COMPARISON && resourceCount < 2) {
				return new AnswerabilityDecision(false, 0.0D, "COMPARISON_REQUIRES_MULTIPLE_DOCUMENTS",
						ProjectRoomAnswerabilityStatus.NEEDS_CLARIFICATION);
			}
			ProjectRoomAnswerabilityStatus status = analysis.intent() == ProjectRoomQueryIntent.DOCUMENT_OVERVIEW
					? ProjectRoomAnswerabilityStatus.ANSWERABLE
					: ProjectRoomAnswerabilityStatus.PARTIALLY_ANSWERABLE;
			return new AnswerabilityDecision(true, 0.72D, "GROUNDED_DOCUMENT_SYNTHESIS", status);
		}
		if (top.retrievalMode().contains("TITLE_SCOPED")) {
			return new AnswerabilityDecision(false, 0.0D, "LOW_TITLE_SCOPED_ANSWERABILITY",
					ProjectRoomAnswerabilityStatus.NO_EVIDENCE);
		}
		double keywordCoverage = keywordCoverage(analysis, selected);
		double normalizedFusionScore = Math.min(1.0D, top.fusionScore() / 1.2D);
		double semanticSignal = top.retrievalMode().contains("SEMANTIC") && top.originalScore() >= 0.68D ? 0.15D : 0.0D;
		double evidenceSignal = selected.size() >= 2 ? 0.05D : 0.0D;
		double score = (normalizedFusionScore * 0.45D)
				+ (keywordCoverage * 0.35D)
				+ semanticSignal
				+ evidenceSignal;
		double minimumKeywordCoverage = AgentQuerySupport.isJapaneseLocale(analysis.locale())
				? MIN_JAPANESE_LEXICAL_ANSWERABILITY_COVERAGE
				: MIN_LEXICAL_ANSWERABILITY_COVERAGE;
		boolean answerable = score >= ANSWERABILITY_MIN_SCORE
				&& ((keywordCoverage >= minimumKeywordCoverage && hasSpecificLexicalEvidence(selected))
				|| semanticSignal > 0.0D);
		return new AnswerabilityDecision(
				answerable,
				Math.round(score * 1000.0D) / 1000.0D,
				answerable ? "ANSWERABILITY_GATE_PASSED" : "LOW_ANSWERABILITY",
				answerable ? ProjectRoomAnswerabilityStatus.ANSWERABLE : ProjectRoomAnswerabilityStatus.NO_EVIDENCE
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

	private boolean isDocumentOverviewRepresentative(
			AgentSearchQueryAnalysis analysis,
			ProjectRoomDocumentCandidate top
	) {
		return top.retrievalMode().equals("REPRESENTATIVE")
				&& analysis.intent() == ProjectRoomQueryIntent.DOCUMENT_OVERVIEW;
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
			AgentSearchQueryAnalysis analysis,
			ProjectRoomDocumentCandidate top,
			List<ProjectRoomDocumentCandidate> selected
	) {
		return top.retrievalMode().contains("TITLE_SCOPED")
				&& contentMatchedKeywordCount(top, selected) >= TITLE_SCOPED_MIN_MATCHED_KEYWORDS
				&& keywordCoverage(analysis, selected) >= TITLE_SCOPED_MIN_KEYWORD_COVERAGE;
	}

	private long contentMatchedKeywordCount(
			ProjectRoomDocumentCandidate candidate,
			List<ProjectRoomDocumentCandidate> selected
	) {
		return candidate.matchedKeywords().stream()
				.filter(keyword -> !isDocumentTitleKeyword(keyword, selected))
				.count();
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
			List<ProjectRoomDocumentCandidate> ranked = resourceBalancedRank(modeCandidates);
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

	/**
	 * Interleaves resources before taking another chunk from the same document.
	 * This keeps RRF from rewarding a single long document merely because it
	 * occupies most of an over-fetched retrieval list.
	 */
	private List<ProjectRoomDocumentCandidate> resourceBalancedRank(
			List<ProjectRoomDocumentCandidate> candidates
	) {
		List<ProjectRoomDocumentCandidate> scoreRanked = candidates.stream()
				.sorted(Comparator.comparingDouble(ProjectRoomDocumentCandidate::originalScore).reversed()
						.thenComparing(candidate -> candidate.hit().chunkIndex()))
				.toList();
		Map<UUID, List<ProjectRoomDocumentCandidate>> candidatesByResource = new LinkedHashMap<>();
		for (ProjectRoomDocumentCandidate candidate : scoreRanked) {
			candidatesByResource.computeIfAbsent(candidate.hit().resourceId(), ignored -> new ArrayList<>())
					.add(candidate);
		}
		List<ProjectRoomDocumentCandidate> balanced = new ArrayList<>(scoreRanked.size());
		for (int resourceRank = 0; balanced.size() < scoreRanked.size(); resourceRank++) {
			for (List<ProjectRoomDocumentCandidate> resourceCandidates : candidatesByResource.values()) {
				if (resourceRank < resourceCandidates.size()) {
					balanced.add(resourceCandidates.get(resourceRank));
				}
			}
		}
		return balanced;
	}

	private double keywordCoverage(
			AgentSearchQueryAnalysis analysis,
			List<ProjectRoomDocumentCandidate> selected
	) {
		List<List<String>> keywordGroups = analysis.rankingKeywordGroups();
		if (keywordGroups.isEmpty()) {
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
		return keywordGroups.stream()
				.mapToDouble(group -> {
					List<String> expected = group.stream()
							.map(AgentQuerySupport::compactResourceText)
							.filter(keyword -> !keyword.isBlank())
							.filter(keyword -> !isDocumentTitleKeyword(keyword, selected))
							.distinct()
							.toList();
					if (expected.isEmpty()) {
						return 0.0D;
					}
					long matchedCount = expected.stream().filter(matched::contains).count();
					return (double) matchedCount / expected.size();
				})
				.max()
				.orElse(0.0D);
	}

	private boolean isDocumentTitleKeyword(
			String keyword,
			List<ProjectRoomDocumentCandidate> selected
	) {
		String compactKeyword = AgentQuerySupport.compactResourceText(keyword);
		if (compactKeyword.isBlank()) {
			return false;
		}
		return selected.stream()
				.map(candidate -> candidate.hit().originalName())
				.filter(name -> name != null && !name.isBlank())
				.map(AgentQuerySupport::compactResourceText)
				.anyMatch(title -> title.contains(compactKeyword));
	}

	private List<ProjectRoomDocumentCandidate> preferSpecificEvidenceCandidates(
			List<ProjectRoomDocumentCandidate> ranked
	) {
		List<ProjectRoomDocumentCandidate> specific = ranked.stream()
				.filter(candidate -> hasSpecificLexicalEvidence(candidate)
						|| (candidate.retrievalMode().contains("SEMANTIC") && candidate.originalScore() >= 0.68D)
						|| candidate.retrievalMode().contains("REPRESENTATIVE"))
				.toList();
		return specific.isEmpty() ? ranked : specific;
	}

	private boolean hasSpecificLexicalEvidence(List<ProjectRoomDocumentCandidate> candidates) {
		return candidates.stream().anyMatch(this::hasSpecificLexicalEvidence);
	}

	private boolean hasSpecificLexicalEvidence(ProjectRoomDocumentCandidate candidate) {
		return candidate.matchedKeywords().stream()
				.anyMatch(keyword -> !AgentQuerySupport.isGenericAnswerabilityKeyword(keyword));
	}

	private boolean hasExclusionOnlyAnchorEvidence(
			AgentSearchQueryAnalysis analysis,
			List<ProjectRoomDocumentCandidate> selected
	) {
		if (analysis.rankingKeywords().isEmpty()) {
			return false;
		}
		String anchor = AgentQuerySupport.normalize(analysis.rankingKeywords().getFirst());
		boolean excluded = false;
		boolean supported = false;
		for (ProjectRoomDocumentCandidate candidate : selected) {
			if (candidate.matchedKeywords().stream()
					.noneMatch(keyword -> AgentQuerySupport.normalize(keyword).equals(anchor))) {
				continue;
			}
			String text = AgentQuerySupport.normalize(candidate.hit().chunkText());
			if (hasAnchorNearExclusionMarker(text, anchor)) {
				excluded = true;
			}
			if (hasAnchorOutsideExclusionMarker(text, anchor)) {
				supported = true;
			}
		}
		return excluded && !supported;
	}

	private boolean hasAnchorNearExclusionMarker(String text, String anchor) {
		return anchorPositions(text, anchor).stream()
				.anyMatch(anchorPosition -> exclusionMarkerPositions(text).stream()
						.anyMatch(markerPosition -> anchorPosition >= markerPosition - 30
								&& anchorPosition <= markerPosition + 180));
	}

	private boolean hasAnchorOutsideExclusionMarker(String text, String anchor) {
		List<Integer> markerPositions = exclusionMarkerPositions(text);
		return anchorPositions(text, anchor).stream()
				.anyMatch(anchorPosition -> markerPositions.stream()
						.noneMatch(markerPosition -> anchorPosition >= markerPosition - 30
								&& anchorPosition <= markerPosition + 180));
	}

	private List<Integer> anchorPositions(String text, String anchor) {
		List<Integer> positions = new ArrayList<>();
		for (int position = text.indexOf(anchor); position >= 0; position = text.indexOf(anchor, position + 1)) {
			positions.add(position);
		}
		return positions;
	}

	private List<Integer> exclusionMarkerPositions(String text) {
		List<Integer> positions = new ArrayList<>();
		for (String marker : List.of(
				"제외 범위", "범위에서 제외", "지원하지 않", "out of scope",
				"excluded", "not supported", "対象外", "含まれません"
		)) {
			for (int position = text.indexOf(marker); position >= 0; position = text.indexOf(marker, position + 1)) {
				positions.add(position);
			}
		}
		return positions;
	}

	private boolean hasHardMatch(ProjectRoomDocumentCandidate candidate, AgentSearchQueryAnalysis analysis) {
		if (analysis.hasPreciseIdentifier() && candidate.matchReason().contains("REQUIREMENT_ID_MATCH")) {
			return true;
		}
		return !analysis.quotedPhrases().isEmpty() && candidate.matchReason().contains("QUOTED_PHRASE_MATCH");
	}

	private List<ProjectRoomDocumentCandidate> selectDiverseCandidates(
			List<ProjectRoomDocumentCandidate> ranked,
			int limit,
			AgentSearchQueryAnalysis analysis
	) {
		List<ProjectRoomDocumentCandidate> selected = new ArrayList<>();
		Map<UUID, Integer> countByResource = new LinkedHashMap<>();
		List<ProjectRoomDocumentCandidate> remaining = new ArrayList<>(ranked);
		while (selected.size() < limit && !remaining.isEmpty()) {
			ProjectRoomDocumentCandidate bestCandidate = null;
			double bestScore = Double.NEGATIVE_INFINITY;
			for (ProjectRoomDocumentCandidate candidate : remaining) {
				UUID resourceId = candidate.hit().resourceId();
				int perResourceLimit = analysis.intent().allowsDocumentSynthesis()
						&& analysis.scopeConfidence().isConfident() ? limit : PER_RESOURCE_LIMIT;
				if (countByResource.getOrDefault(resourceId, 0) >= perResourceLimit) {
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
			String answerabilityReason,
			ProjectRoomAnswerabilityStatus answerabilityStatus
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
			String reason,
			ProjectRoomAnswerabilityStatus status
	) {
	}
}
