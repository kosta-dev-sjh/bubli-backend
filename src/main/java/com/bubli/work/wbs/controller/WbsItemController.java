package com.bubli.work.wbs.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.response.PageResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.work.wbs.dto.CreateWbsItemRequest;
import com.bubli.work.wbs.dto.UpdateWbsItemRequest;
import com.bubli.work.wbs.dto.WbsItemResponse;
import com.bubli.work.wbs.dto.WbsItemResult;
import com.bubli.work.wbs.service.WbsItemService;
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
public class WbsItemController {

	private final WbsItemService wbsItemService;

	@GetMapping("/api/project-rooms/{roomId}/wbs-items")
	public ApiResponse<PageResponse<WbsItemResponse>> getRoomWbsItems(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@PageableDefault(size = 50) Pageable pageable
	) {
		return ApiResponse.success(mapPage(wbsItemService.getRoomWbsItems(authUser.userId(), roomId, pageable)));
	}

	@PostMapping("/api/project-rooms/{roomId}/wbs-items")
	public ApiResponse<WbsItemResponse> createWbsItem(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@Valid @RequestBody CreateWbsItemRequest request
	) {
		return ApiResponse.success(WbsItemResponse.from(wbsItemService.create(authUser.userId(), roomId, request)));
	}

	@PatchMapping("/api/wbs-items/{wbsItemId}")
	public ApiResponse<WbsItemResponse> updateWbsItem(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID wbsItemId,
			@Valid @RequestBody UpdateWbsItemRequest request
	) {
		return ApiResponse.success(WbsItemResponse.from(wbsItemService.update(authUser.userId(), wbsItemId, request)));
	}

	@DeleteMapping("/api/wbs-items/{wbsItemId}")
	public ApiResponse<Void> deleteWbsItem(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID wbsItemId
	) {
		wbsItemService.delete(authUser.userId(), wbsItemId);
		return ApiResponse.success(null);
	}

	private PageResponse<WbsItemResponse> mapPage(PageResponse<WbsItemResult> page) {
		return new PageResponse<>(
				page.getItems().stream()
						.map(WbsItemResponse::from)
						.toList(),
				page.getPage(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isHasNext()
		);
	}
}
