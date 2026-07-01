package com.bubli.personal.memo.dto;

import com.bubli.personal.memo.type.MemoStatus;

import java.time.Instant;
import java.util.UUID;

public record MemoResponse(
		UUID id,
		UUID authorUserId,
		UUID roomId,
		String body,
		MemoStatus status,
		Instant createdAt,
		Instant updatedAt
) {

	public static MemoResponse from(MemoResult result) {
		return new MemoResponse(
				result.id(),
				result.authorUserId(),
				result.roomId(),
				result.body(),
				result.status(),
				result.createdAt(),
				result.updatedAt()
		);
	}
}
