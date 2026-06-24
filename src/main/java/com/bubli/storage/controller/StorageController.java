package com.bubli.storage.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.storage.dto.StorageUsageResponse;
import com.bubli.storage.service.StorageUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StorageController {

	private final StorageUsageService storageUsageService;

	@GetMapping("/api/storage/usage")
	public ApiResponse<StorageUsageResponse> getMyStorageUsage(@CurrentUser AuthUser authUser) {
		return ApiResponse.success(StorageUsageResponse.from(
				storageUsageService.getMyStorageUsage(authUser.userId())
		));
	}
}
