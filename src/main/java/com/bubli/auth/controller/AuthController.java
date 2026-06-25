package com.bubli.auth.controller;

import com.bubli.auth.dto.AuthLoginRequest;
import com.bubli.auth.dto.AuthTokenResponse;
import com.bubli.auth.dto.RefreshTokenRequest;
import com.bubli.auth.service.AuthService;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/api/auth/login")
	public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody AuthLoginRequest request) {
		return ApiResponse.success(authService.login(request.toCommand()));
	}

	@PostMapping("/api/auth/signup")
	public ApiResponse<AuthTokenResponse> signup(@Valid @RequestBody AuthLoginRequest request) {
		// Google 로그인만 사용하므로 signup도 첫 로그인 처리 흐름과 같은 서비스로 연결한다.
		return ApiResponse.success(authService.login(request.toCommand()));
	}

	@PostMapping("/api/auth/refresh")
	public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.success(authService.refresh(request.toCommand()));
	}

	@PostMapping("/api/auth/logout")
	public ApiResponse<Void> logout(@CurrentUser AuthUser authUser) {
		authService.logout(authUser.userId());
		return ApiResponse.success(null);
	}
}
