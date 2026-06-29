package com.bubli.auth.service;

import com.bubli.auth.dto.AuthTokenResponse;
import com.bubli.auth.dto.GoogleAuthorizeCommand;
import com.bubli.auth.dto.GoogleAuthorizeResponse;
import com.bubli.auth.dto.GoogleCallbackCommand;
import com.bubli.auth.dto.GoogleUserProfile;
import com.bubli.auth.dto.RefreshTokenCommand;
import com.bubli.auth.entity.UserSession;
import com.bubli.auth.repository.UserSessionRepository;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.JwtTokenProvider;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.user.dto.MeResponse;
import com.bubli.user.dto.UpsertGoogleUserCommand;
import com.bubli.user.dto.UserResult;
import com.bubli.user.service.UserPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final String GOOGLE_AUTHORIZE_URI = "https://accounts.google.com/o/oauth2/v2/auth";

	private final GoogleOAuthClient googleOAuthClient;
	private final JwtTokenProvider jwtTokenProvider;
	private final UserPublicService userPublicService;
	private final UserSessionRepository userSessionRepository;

	@Value("${google.oauth.client-id:${google.calendar.client-id:}}")
	private String googleClientId;

	@Value("${google.oauth.redirect-uri:http://localhost:3000/auth/callback}")
	private String defaultRedirectUri;

	@Transactional(readOnly = true)
	public GoogleAuthorizeResponse createGoogleAuthorizeUrl(GoogleAuthorizeCommand command) {
		String redirectUri = hasText(command.redirectUri()) ? command.redirectUri() : defaultRedirectUri;
		String state = hasText(command.state()) ? command.state() : command.clientType().name().toLowerCase(Locale.ROOT);
		String authorizeUrl = UriComponentsBuilder.fromUriString(GOOGLE_AUTHORIZE_URI)
				.queryParam("client_id", googleClientId)
				.queryParam("redirect_uri", redirectUri)
				.queryParam("response_type", "code")
				.queryParam("scope", "openid profile")
				.queryParam("access_type", "offline")
				.queryParam("prompt", "select_account")
				.queryParam("state", state)
				.build()
				.toUriString();
		return new GoogleAuthorizeResponse(authorizeUrl);
	}

	@Transactional
	public AuthTokenResponse handleGoogleCallback(GoogleCallbackCommand command) {
		GoogleUserProfile profile = googleOAuthClient.fetchUserProfile(command);
		UserResult user = userPublicService.upsertGoogleUser(new UpsertGoogleUserCommand(
				profile.sub(),
				profile.name(),
				profile.picture(),
				profile.locale(),
				null
		));
		return issueTokens(user, command.clientType());
	}

	@Transactional
	public AuthTokenResponse refresh(RefreshTokenCommand command) {
		AuthUser authUser;
		try {
			authUser = jwtTokenProvider.getAuthUser(command.refreshToken());
		} catch (RuntimeException e) {
			throw new BusinessException(ErrorCode.AUTH_401_006);
		}

		UserSession session = userSessionRepository.findByRefreshTokenAndClientType(
						hashToken(command.refreshToken()),
						command.clientType()
				)
				.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_401_006));
		Instant now = Instant.now();
		if (!session.isActiveAt(now)) {
			throw new BusinessException(ErrorCode.AUTH_401_007);
		}
		if (!session.getUserId().equals(authUser.userId())) {
			session.revoke();
			throw new BusinessException(ErrorCode.AUTH_401_006);
		}

		UserResult user = userPublicService.getUser(authUser.userId());
		return rotateTokens(user, session);
	}

	@Transactional
	public void logout(UUID userId, RefreshTokenCommand command) {
		AuthUser authUser;
		try {
			authUser = jwtTokenProvider.getAuthUser(command.refreshToken());
		} catch (RuntimeException e) {
			throw new BusinessException(ErrorCode.AUTH_401_006);
		}
		if (!userId.equals(authUser.userId())) {
			throw new BusinessException(ErrorCode.AUTH_401_006);
		}

		UserSession session = userSessionRepository.findByRefreshTokenAndClientType(
						hashToken(command.refreshToken()),
						command.clientType()
				)
				.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_401_006));
		if (!session.getUserId().equals(userId)) {
			throw new BusinessException(ErrorCode.AUTH_401_006);
		}
		session.revoke();
	}

	private AuthTokenResponse issueTokens(UserResult user, com.bubli.auth.type.ClientType clientType) {
		AuthUser authUser = new AuthUser(user.id());
		String accessToken = jwtTokenProvider.createAccessToken(authUser);
		String refreshToken = jwtTokenProvider.createRefreshToken(authUser);
		Instant refreshTokenExpiresAt = Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpireMs());

		UserSession session = userSessionRepository.findByUserIdAndClientType(user.id(), clientType)
				.orElseGet(() -> UserSession.create(user.id(), hashToken(refreshToken), clientType, refreshTokenExpiresAt));
		session.rotate(hashToken(refreshToken), refreshTokenExpiresAt);
		userSessionRepository.save(session);

		return toTokenResponse(user, accessToken, refreshToken, refreshTokenExpiresAt);
	}

	private AuthTokenResponse rotateTokens(UserResult user, UserSession session) {
		AuthUser authUser = new AuthUser(user.id());
		String accessToken = jwtTokenProvider.createAccessToken(authUser);
		String refreshToken = jwtTokenProvider.createRefreshToken(authUser);
		Instant refreshTokenExpiresAt = Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpireMs());
		session.rotate(hashToken(refreshToken), refreshTokenExpiresAt);
		return toTokenResponse(user, accessToken, refreshToken, refreshTokenExpiresAt);
	}

	private AuthTokenResponse toTokenResponse(
			UserResult user,
			String accessToken,
			String refreshToken,
			Instant refreshTokenExpiresAt
	) {
		Instant accessTokenExpiresAt = Instant.now().plusMillis(jwtTokenProvider.getAccessTokenExpireMs());
		return new AuthTokenResponse(
				accessToken,
				"Bearer",
				jwtTokenProvider.getAccessTokenExpireMs() / 1000,
				accessTokenExpiresAt,
				refreshToken,
				refreshTokenExpiresAt,
				MeResponse.from(new UserResult(
						user.id(),
						user.bubliId(),
						user.name(),
						user.avatarUrl(),
						user.locale(),
						user.timezone()
				))
		);
	}

	private String hashToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new BusinessException(ErrorCode.COMMON_500_001);
		}
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
