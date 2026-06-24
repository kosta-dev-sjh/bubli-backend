package com.bubli.auth.controller;

import com.bubli.auth.dto.AuthTokenResponse;
import com.bubli.auth.dto.GoogleAuthorizeResponse;
import com.bubli.auth.dto.GoogleCallbackRequest;
import com.bubli.auth.dto.RefreshTokenRequest;
import com.bubli.auth.service.AuthService;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@GetMapping("/api/auth/google/authorize")
	public ApiResponse<GoogleAuthorizeResponse> authorizeGoogle() {
		return ApiResponse.success(authService.createGoogleAuthorizeUrl());
	}

	@PostMapping("/api/auth/google/callback")
	public ApiResponse<AuthTokenResponse> callbackGoogle(@Valid @RequestBody GoogleCallbackRequest request) {
		return ApiResponse.success(authService.handleGoogleCallback(request));
	}

	@PostMapping("/api/auth/refresh")
	public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.success(authService.refresh(request));
	}

	@PostMapping("/api/auth/logout")
	public ApiResponse<Void> logout(@CurrentUser AuthUser authUser) {
		authService.logout(authUser.userId());
		return ApiResponse.success(null);
	}
}
