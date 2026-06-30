package com.bubli.agent.dto;

import com.bubli.chat.dto.ChatMessageResponse;
import com.bubli.memory.dto.RoomMemorySummaryContextResult;

public record ProjectRoomAgentCommandResponse(
		ChatMessageResponse message,
		RoomMemorySummaryContextResult memorySummary
) {
}
