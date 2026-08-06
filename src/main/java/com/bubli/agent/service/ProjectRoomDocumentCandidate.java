package com.bubli.agent.service;

import com.bubli.resource.dto.ResourceSearchHit;

import java.util.ArrayList;
import java.util.List;

record ProjectRoomDocumentCandidate(
		ResourceSearchHit hit,
		String retrievalMode,
		double originalScore,
		double fusionScore,
		List<String> matchedKeywords,
		String matchReason
) {

	ProjectRoomDocumentCandidate {
		matchedKeywords = matchedKeywords == null ? List.of() : List.copyOf(matchedKeywords);
		matchReason = matchReason == null ? retrievalMode : matchReason;
	}

	static ProjectRoomDocumentCandidate of(
			ResourceSearchHit hit,
			String retrievalMode,
			AgentSearchQueryAnalysis analysis,
			boolean titleScoped
	) {
		List<String> matchedKeywords = matchedKeywords(hit, analysis);
		double score = baseStrategyScore(retrievalMode, hit.similarityScore());
		if (titleScoped) {
			score += 0.20D;
		}
		if (!matchedKeywords.isEmpty()) {
			score += Math.min(0.30D, matchedKeywords.size() * 0.08D);
		}
		if (analysis.hasPreciseIdentifier() && containsAny(hit.chunkText(), analysis.requirementIdentifiers())) {
			score += 0.35D;
		}
		if (!analysis.quotedPhrases().isEmpty() && containsAny(hit.chunkText(), analysis.quotedPhrases())) {
			score += 0.25D;
		}
		return new ProjectRoomDocumentCandidate(
				hit,
				retrievalMode,
				hit.similarityScore(),
				Math.min(2.0D, score),
				matchedKeywords,
				matchReason(retrievalMode, titleScoped, matchedKeywords, analysis, hit)
		);
	}

	ProjectRoomDocumentCandidate merge(ProjectRoomDocumentCandidate other) {
		List<String> mergedKeywords = new ArrayList<>(matchedKeywords);
		for (String keyword : other.matchedKeywords) {
			if (!mergedKeywords.contains(keyword)) {
				mergedKeywords.add(keyword);
			}
		}
		String mergedMode = retrievalMode.equals(other.retrievalMode)
				? retrievalMode
				: retrievalMode + "+" + other.retrievalMode;
		String mergedReason = matchReason.equals(other.matchReason)
				? matchReason
				: matchReason + "; " + other.matchReason;
		ResourceSearchHit bestHit = fusionScore >= other.fusionScore ? hit : other.hit;
		return new ProjectRoomDocumentCandidate(
				bestHit,
				mergedMode,
				Math.max(originalScore, other.originalScore),
				Math.max(fusionScore, other.fusionScore) + 0.05D,
				mergedKeywords,
				mergedReason
		);
	}

	private static double baseStrategyScore(String retrievalMode, double originalScore) {
		return switch (retrievalMode) {
			case "TITLE_SCOPED_SEMANTIC" -> originalScore + 0.18D;
			case "TITLE_SCOPED_KEYWORD" -> 0.72D + originalScore * 0.35D;
			case "KEYWORD" -> 0.62D + originalScore * 0.35D;
			case "REPRESENTATIVE" -> 0.58D + originalScore * 0.10D;
			default -> originalScore;
		};
	}

	private static List<String> matchedKeywords(ResourceSearchHit hit, AgentSearchQueryAnalysis analysis) {
		String compactChunk = AgentQuerySupport.compactResourceText(hit.chunkText());
		List<String> matches = new ArrayList<>();
		for (String keyword : analysis.rankingKeywords()) {
			String compactKeyword = AgentQuerySupport.compactResourceText(keyword);
			if (!compactKeyword.isBlank() && compactChunk.contains(compactKeyword) && !matches.contains(keyword)) {
				matches.add(keyword);
			}
		}
		return matches;
	}

	private static boolean containsAny(String value, List<String> needles) {
		String compactValue = AgentQuerySupport.compactResourceText(value);
		for (String needle : needles) {
			String compactNeedle = AgentQuerySupport.compactResourceText(needle);
			if (!compactNeedle.isBlank() && compactValue.contains(compactNeedle)) {
				return true;
			}
		}
		return false;
	}

	private static String matchReason(
			String retrievalMode,
			boolean titleScoped,
			List<String> matchedKeywords,
			AgentSearchQueryAnalysis analysis,
			ResourceSearchHit hit
	) {
		List<String> reasons = new ArrayList<>();
		reasons.add(retrievalMode);
		if (titleScoped) {
			reasons.add("TITLE_MATCHED_RESOURCE");
		}
		if (!matchedKeywords.isEmpty()) {
			reasons.add("KEYWORD_MATCH");
		}
		if (analysis.hasPreciseIdentifier() && containsAny(hit.chunkText(), analysis.requirementIdentifiers())) {
			reasons.add("REQUIREMENT_ID_MATCH");
		}
		if (!analysis.quotedPhrases().isEmpty() && containsAny(hit.chunkText(), analysis.quotedPhrases())) {
			reasons.add("QUOTED_PHRASE_MATCH");
		}
		return String.join(",", reasons);
	}
}
