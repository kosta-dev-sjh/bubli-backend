package com.bubli.auth.controller;

import com.bubli.auth.dto.AuthTokenResponse;
import com.bubli.auth.dto.GoogleAuthorizeCommand;
import com.bubli.auth.dto.GoogleAuthorizeResponse;
import com.bubli.auth.dto.GoogleCallbackRequest;
import com.bubli.auth.dto.LogoutRequest;
import com.bubli.auth.dto.RefreshTokenRequest;
import com.bubli.auth.type.ClientType;
import com.bubli.auth.service.AuthService;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@GetMapping("/api/auth/google/authorize")
	public ApiResponse<GoogleAuthorizeResponse> authorizeGoogle(
			@RequestParam(required = false) String redirectUri,
			@RequestParam(required = false, defaultValue = "TAURI") ClientType clientType,
			@RequestParam(required = false) String state
	) {
		return ApiResponse.success(authService.createGoogleAuthorizeUrl(
				new GoogleAuthorizeCommand(redirectUri, clientType, state)
		));
	}

	@GetMapping("/oauth2/authorization/google")
	public ResponseEntity<Void> redirectGoogle(
			@RequestParam(required = false) String redirectUri,
			@RequestParam(required = false, defaultValue = "WEB") ClientType clientType,
			@RequestParam(required = false) String state
	) {
		GoogleAuthorizeResponse response = authService.createGoogleAuthorizeUrl(
				new GoogleAuthorizeCommand(redirectUri, clientType, state)
		);
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(response.authorizeUrl()))
				.build();
	}

	@PostMapping("/api/auth/google/callback")
	public ApiResponse<AuthTokenResponse> callbackGoogle(@Valid @RequestBody GoogleCallbackRequest request) {
		return ApiResponse.success(authService.handleGoogleCallback(request.toCommand()));
	}

	@PostMapping("/api/auth/refresh")
	public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.success(authService.refresh(request.toCommand()));
	}

	@PostMapping("/api/auth/logout")
	public ApiResponse<Void> logout(@CurrentUser AuthUser authUser, @Valid @RequestBody LogoutRequest request) {
		authService.logout(authUser.userId(), request.toCommand());
		return ApiResponse.success(null);
	}
}
