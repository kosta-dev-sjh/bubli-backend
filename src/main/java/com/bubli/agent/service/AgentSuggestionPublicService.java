package com.bubli.agent.service;

import java.util.List;
import java.util.UUID;

public interface AgentSuggestionPublicService {

	List<String> getReviewRequiredSummaries(UUID userId, int limit);
}
