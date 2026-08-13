package com.bubli.agent.service;

enum ProjectRoomQueryIntent {
	GENERAL_DOCUMENT_QA,
	FACT_QA,
	DOCUMENT_OVERVIEW,
	REVIEW_CHECKLIST,
	ROLE_BASED_ANALYSIS,
	DOCUMENT_COMPARISON;

	boolean allowsDocumentSynthesis() {
		return this != GENERAL_DOCUMENT_QA && this != FACT_QA;
	}
}
