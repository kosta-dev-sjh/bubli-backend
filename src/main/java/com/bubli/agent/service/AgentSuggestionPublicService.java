package com.bubli.agent.service;

import com.bubli.agent.dto.AgentSuggestionResponse;

import java.util.List;
import java.util.UUID;

public interface AgentSuggestionPublicService {

	List<String> getReviewRequiredSummaries(UUID userId, int limit);

	List<AgentSuggestionResponse> getRecentRoomSuggestions(UUID userId, UUID roomId, int limit);
}
