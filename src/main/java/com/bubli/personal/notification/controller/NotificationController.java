package com.bubli.personal.notification.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.response.PageResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.personal.notification.dto.NotificationResponse;
import com.bubli.personal.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	@GetMapping("/api/notifications")
	public ApiResponse<PageResponse<NotificationResponse>> getNotifications(
			@CurrentUser AuthUser authUser,
			@PageableDefault(size = 20) Pageable pageable
	) {
		Page<NotificationResponse> page = notificationService.getNotifications(authUser.userId(), pageable);
		return ApiResponse.success(new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext()
		));
	}

	@PatchMapping("/api/notifications/{id}/read")
	public ApiResponse<Void> readNotification(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID id
	) {
		notificationService.readNotification(authUser.userId(), id);
		return ApiResponse.success(null);
	}

	@PatchMapping("/api/notifications/{id}/archive")
	public ApiResponse<Void> archiveNotification(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID id
	) {
		notificationService.archiveNotification(authUser.userId(), id);
		return ApiResponse.success(null);
	}
}
