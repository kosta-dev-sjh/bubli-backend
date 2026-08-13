package com.bubli.agent.service;

enum DocumentScopeConfidence {
	EXPLICIT,
	EXACT_TITLE,
	STRONG_TITLE,
	AMBIGUOUS,
	NONE;

	boolean isConfident() {
		return this == EXPLICIT || this == EXACT_TITLE || this == STRONG_TITLE;
	}
}
