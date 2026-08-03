package com.bubli.agent.service;

import java.util.List;

record AgentSearchQueryAnalysis(
		String normalizedQuery,
		String locale,
		List<String> keywords,
		List<String> requirementIdentifiers,
		List<String> quotedPhrases,
		List<String> titleTokens
) {

	AgentSearchQueryAnalysis {
		normalizedQuery = normalizedQuery == null ? "" : normalizedQuery.trim();
		locale = locale == null || locale.isBlank() ? "ko-KR" : locale;
		keywords = keywords == null ? List.of() : List.copyOf(keywords);
		requirementIdentifiers = requirementIdentifiers == null ? List.of() : List.copyOf(requirementIdentifiers);
		quotedPhrases = quotedPhrases == null ? List.of() : List.copyOf(quotedPhrases);
		titleTokens = titleTokens == null ? List.of() : List.copyOf(titleTokens);
	}

	boolean hasPreciseIdentifier() {
		return !requirementIdentifiers.isEmpty();
	}
}
