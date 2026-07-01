package com.bubli.memory.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.memory.dto.CreateRoomMemorySummaryRequest;
import com.bubli.memory.dto.RoomMemorySummaryContextResult;
import com.bubli.memory.service.RoomMemoryPublicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RoomMemorySummaryController {

	private final RoomMemoryPublicService roomMemoryPublicService;

	@GetMapping("/api/project-rooms/{roomId}/memory-summaries")
	public ApiResponse<List<RoomMemorySummaryContextResult>> getMemorySummaries(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@RequestParam(defaultValue = "5") int limit
	) {
		return ApiResponse.success(roomMemoryPublicService.getRecentRoomMemories(authUser.userId(), roomId, limit));
	}

	@PostMapping("/api/project-rooms/{roomId}/memory-summaries")
	public ApiResponse<RoomMemorySummaryContextResult> createMemorySummary(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@Valid @RequestBody CreateRoomMemorySummaryRequest request
	) {
		return ApiResponse.success(roomMemoryPublicService.createDraft(
				authUser.userId(),
				roomId,
				request.fromSequence(),
				request.toSequence(),
				request.summaryJson()
		));
	}
}
