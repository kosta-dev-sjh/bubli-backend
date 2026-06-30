package com.bubli.memory.dto;

import com.bubli.memory.entity.RoomMemorySummary;
import com.bubli.memory.type.SummaryStatus;

import java.time.Instant;
import java.util.UUID;

public record RoomMemorySummaryContextResult(
		UUID id,
		Long fromSequence,
		Long toSequence,
		String summaryJson,
		SummaryStatus status,
		Instant createdAt
) {

	public static RoomMemorySummaryContextResult from(RoomMemorySummary summary) {
		return new RoomMemorySummaryContextResult(
				summary.getId(),
				summary.getFromSequence(),
				summary.getToSequence(),
				summary.getSummaryJson(),
				summary.getStatus(),
				summary.getCreatedAt()
		);
	}
}
