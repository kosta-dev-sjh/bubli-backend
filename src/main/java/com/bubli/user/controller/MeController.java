package com.bubli.user.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.user.dto.MeResponse;
import com.bubli.user.dto.UpdateMeRequest;
import com.bubli.user.dto.UpdateNotificationPreferencesRequest;
import com.bubli.user.dto.UpdatePrivacyConsentsRequest;
import com.bubli.user.dto.UpdateUserPreferenceRequest;
import com.bubli.user.dto.UserNotificationPreferenceResponse;
import com.bubli.user.dto.UserPreferenceResponse;
import com.bubli.user.dto.UserPrivacyConsentResponse;
import com.bubli.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeController {

	private final UserService userService;

	@GetMapping("/api/me")
	public ApiResponse<MeResponse> getMe(@CurrentUser AuthUser authUser) {
		return ApiResponse.success(MeResponse.from(userService.getMe(authUser.userId(), authUser.email())));
	}

	@PatchMapping("/api/me")
	public ApiResponse<MeResponse> updateMe(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody UpdateMeRequest request
	) {
		return ApiResponse.success(MeResponse.from(userService.updateMe(
				authUser.userId(),
				authUser.email(),
				request.toCommand()
		)));
	}

	@GetMapping("/api/me/preferences")
	public ApiResponse<UserPreferenceResponse> getPreferences(@CurrentUser AuthUser authUser) {
		return ApiResponse.success(UserPreferenceResponse.from(userService.getPreferences(authUser.userId())));
	}

	@PatchMapping("/api/me/preferences")
	public ApiResponse<UserPreferenceResponse> updatePreferences(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody UpdateUserPreferenceRequest request
	) {
		return ApiResponse.success(UserPreferenceResponse.from(userService.updatePreferences(
				authUser.userId(),
				request.toCommand()
		)));
	}

	@GetMapping("/api/me/notification-preferences")
	public ApiResponse<UserNotificationPreferenceResponse> getNotificationPreferences(@CurrentUser AuthUser authUser) {
		return ApiResponse.success(UserNotificationPreferenceResponse.from(
				userService.getNotificationPreferences(authUser.userId())
		));
	}

	@PatchMapping("/api/me/notification-preferences")
	public ApiResponse<UserNotificationPreferenceResponse> updateNotificationPreferences(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody UpdateNotificationPreferencesRequest request
	) {
		return ApiResponse.success(UserNotificationPreferenceResponse.from(
				userService.updateNotificationPreferences(authUser.userId(), request.toCommand())
		));
	}

	@GetMapping("/api/me/privacy-consents")
	public ApiResponse<UserPrivacyConsentResponse> getPrivacyConsents(@CurrentUser AuthUser authUser) {
		return ApiResponse.success(UserPrivacyConsentResponse.from(
				userService.getPrivacyConsents(authUser.userId())
		));
	}

	@PatchMapping("/api/me/privacy-consents")
	public ApiResponse<UserPrivacyConsentResponse> updatePrivacyConsents(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody UpdatePrivacyConsentsRequest request
	) {
		return ApiResponse.success(UserPrivacyConsentResponse.from(
				userService.updatePrivacyConsents(authUser.userId(), request.toCommand())
		));
	}
}
