package com.bubli.personal.memo.service;

import com.bubli.personal.memo.dto.CreateMemoCommand;
import com.bubli.personal.memo.dto.MemoResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MemoPublicService {

	MemoResult createPersonalMemo(UUID userId, CreateMemoCommand command);

	MemoResult createRoomMemo(UUID userId, UUID roomId, CreateMemoCommand command);

	List<MemoResult> getUpdatedMemosBetween(UUID userId, Instant from, Instant to, int limit);
}
