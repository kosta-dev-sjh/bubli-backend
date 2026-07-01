package com.bubli.personal.memo.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.response.PageResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.personal.memo.dto.CreateMemoRequest;
import com.bubli.personal.memo.dto.MemoResponse;
import com.bubli.personal.memo.dto.MemoResult;
import com.bubli.personal.memo.dto.UpdateMemoRequest;
import com.bubli.personal.memo.service.MemoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MemoController {

	private final MemoService memoService;

	@GetMapping("/api/memos")
	public ApiResponse<PageResponse<MemoResponse>> getPersonalMemos(
			@CurrentUser AuthUser authUser,
			@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(mapPage(memoService.getPersonalMemos(authUser.userId(), pageable)));
	}

	@PostMapping("/api/memos")
	public ApiResponse<MemoResponse> createPersonalMemo(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody CreateMemoRequest request
	) {
		return ApiResponse.success(MemoResponse.from(memoService.createPersonalMemo(authUser.userId(), request.toCommand())));
	}

	@GetMapping("/api/project-rooms/{roomId}/memos")
	public ApiResponse<PageResponse<MemoResponse>> getRoomMemos(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(mapPage(memoService.getRoomMemos(authUser.userId(), roomId, pageable)));
	}

	@PostMapping("/api/project-rooms/{roomId}/memos")
	public ApiResponse<MemoResponse> createRoomMemo(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@Valid @RequestBody CreateMemoRequest request
	) {
		return ApiResponse.success(MemoResponse.from(memoService.createRoomMemo(authUser.userId(), roomId, request.toCommand())));
	}

	@PatchMapping("/api/memos/{memoId}")
	public ApiResponse<MemoResponse> updateMemo(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID memoId,
			@Valid @RequestBody UpdateMemoRequest request
	) {
		return ApiResponse.success(MemoResponse.from(memoService.updateMemo(authUser.userId(), memoId, request.toCommand())));
	}

	@DeleteMapping("/api/memos/{memoId}")
	public ApiResponse<Void> deleteMemo(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID memoId
	) {
		memoService.deleteMemo(authUser.userId(), memoId);
		return ApiResponse.success(null);
	}

	private PageResponse<MemoResponse> mapPage(PageResponse<MemoResult> page) {
		return new PageResponse<>(
				page.getItems().stream()
						.map(MemoResponse::from)
						.toList(),
				page.getPage(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isHasNext()
		);
	}
}
