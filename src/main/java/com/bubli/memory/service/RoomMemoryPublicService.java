package com.bubli.memory.service;

import com.bubli.memory.dto.RoomMemorySummaryContextResult;

import java.util.List;
import java.util.UUID;

public interface RoomMemoryPublicService {

	List<RoomMemorySummaryContextResult> getRecentRoomMemories(UUID userId, UUID roomId, int limit);

	RoomMemorySummaryContextResult createDraft(UUID userId, UUID roomId, Long fromSequence, Long toSequence, String summaryJson);
}
