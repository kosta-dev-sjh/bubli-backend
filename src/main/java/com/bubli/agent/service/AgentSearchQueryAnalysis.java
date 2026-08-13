package com.bubli.agent.service;

import java.util.List;

record AgentSearchQueryAnalysis(
		String normalizedQuery,
		String locale,
		List<String> keywords,
		List<String> requirementIdentifiers,
		List<String> quotedPhrases,
		List<String> titleTokens,
		List<List<String>> keywordGroups,
		ProjectRoomQueryIntent intent,
		DocumentScopeConfidence scopeConfidence,
		String perspective
) {

	AgentSearchQueryAnalysis {
		normalizedQuery = normalizedQuery == null ? "" : normalizedQuery.trim();
		locale = locale == null || locale.isBlank() ? "ko-KR" : locale;
		keywords = keywords == null ? List.of() : List.copyOf(keywords);
		requirementIdentifiers = requirementIdentifiers == null ? List.of() : List.copyOf(requirementIdentifiers);
		quotedPhrases = quotedPhrases == null ? List.of() : List.copyOf(quotedPhrases);
		titleTokens = titleTokens == null ? List.of() : List.copyOf(titleTokens);
		keywordGroups = keywordGroups == null || keywordGroups.isEmpty()
				? (keywords.isEmpty() ? List.of() : List.of(keywords))
				: keywordGroups.stream()
						.filter(group -> group != null && !group.isEmpty())
						.map(List::copyOf)
						.toList();
		intent = intent == null ? ProjectRoomQueryIntent.GENERAL_DOCUMENT_QA : intent;
		scopeConfidence = scopeConfidence == null ? DocumentScopeConfidence.NONE : scopeConfidence;
		perspective = perspective == null ? "" : perspective.trim();
	}

	AgentSearchQueryAnalysis(
			String normalizedQuery,
			String locale,
			List<String> keywords,
			List<String> requirementIdentifiers,
			List<String> quotedPhrases,
			List<String> titleTokens,
			List<List<String>> keywordGroups
	) {
		this(normalizedQuery, locale, keywords, requirementIdentifiers, quotedPhrases, titleTokens,
				keywordGroups, ProjectRoomQueryIntent.GENERAL_DOCUMENT_QA, DocumentScopeConfidence.NONE, "");
	}

	AgentSearchQueryAnalysis withScopeConfidence(DocumentScopeConfidence confidence) {
		return new AgentSearchQueryAnalysis(
				normalizedQuery, locale, keywords, requirementIdentifiers, quotedPhrases, titleTokens,
				keywordGroups, intent, confidence, perspective
		);
	}

	boolean hasPreciseIdentifier() {
		return !requirementIdentifiers.isEmpty();
	}

	List<String> rankingKeywords() {
		return rankingKeywordGroups().stream()
				.flatMap(List::stream)
				.distinct()
				.toList();
	}

	List<List<String>> rankingKeywordGroups() {
		return keywordGroups.stream()
				.map(group -> group.stream()
				.filter(keyword -> !AgentQuerySupport.isAnswerabilityStopword(keyword))
				.distinct()
				.toList())
				.filter(group -> !group.isEmpty())
				.toList();
	}
}
