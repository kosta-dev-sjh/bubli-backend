package com.bubli.personal.memo.dto;

import com.bubli.personal.memo.entity.Memo;
import com.bubli.personal.memo.type.MemoStatus;

import java.time.Instant;
import java.util.UUID;

public record MemoResult(
		UUID id,
		UUID authorUserId,
		UUID roomId,
		String body,
		MemoStatus status,
		Instant createdAt,
		Instant updatedAt
) {

	public static MemoResult from(Memo memo) {
		return new MemoResult(
				memo.getId(),
				memo.getAuthorUserId(),
				memo.getRoomId(),
				memo.getBody(),
				memo.getStatus(),
				memo.getCreatedAt(),
				memo.getUpdatedAt()
		);
	}
}
