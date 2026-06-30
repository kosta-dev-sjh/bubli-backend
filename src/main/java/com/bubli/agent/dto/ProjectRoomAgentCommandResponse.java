package com.bubli.agent.dto;

import com.bubli.chat.dto.ChatMessageResponse;
import com.bubli.memory.dto.RoomMemorySummaryContextResult;

import java.util.List;

public record ProjectRoomAgentCommandResponse(
		ChatMessageResponse message,
		RoomMemorySummaryContextResult memorySummary,
		List<AgentSuggestionResponse> suggestions
) {

	public ProjectRoomAgentCommandResponse {
		suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
	}
}
