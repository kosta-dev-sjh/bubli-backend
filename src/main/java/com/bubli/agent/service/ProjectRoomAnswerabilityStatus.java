package com.bubli.agent.service;

enum ProjectRoomAnswerabilityStatus {
	ANSWERABLE,
	PARTIALLY_ANSWERABLE,
	NEEDS_CLARIFICATION,
	NO_EVIDENCE,
	RETRIEVAL_FAILED;

	boolean canAnswer() {
		return this == ANSWERABLE || this == PARTIALLY_ANSWERABLE;
	}
}
