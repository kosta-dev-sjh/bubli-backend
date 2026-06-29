package com.bubli.auth.dto;

import com.bubli.user.dto.MeResponse;

import java.time.Instant;

public record AuthTokenResponse(
		String accessToken,
		String tokenType,
		long expiresIn,
		Instant expiresAt,
		String refreshToken,
		Instant refreshTokenExpiresAt,
		MeResponse user
) {
}
