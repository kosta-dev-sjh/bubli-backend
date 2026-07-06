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
import com.bubli.personal.calendar.service.GoogleCalendarConnectionPublicService;
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
	// 로그인 스코프에 전체 calendar를 포함한다. 로그인만 한 계정도 프로젝트룸 이름의 전용
	// 구글 캘린더를 만들 수 있어야, 룸 일정이 그 캘린더로 저장된다(calendar.events만으로는 캘린더 생성 불가).
	// (#187에서 calendar를 넣었다가 동의 화면 미등록 스코프로 로그인이 거부된 적이 있으나, 지금은
	//  같은 OAuth 클라이언트의 캘린더 "연결" 플로우(CALENDAR_SCOPE)가 이미 calendar 스코프로 동작 중이라
	//  동의 화면에 등록되어 있다. 로그인/캘린더 연결은 google.oauth.client-id == google.calendar.client-id로 동일 클라이언트.)
	private static final String GOOGLE_LOGIN_SCOPE =
			"openid profile email https://www.googleapis.com/auth/calendar "
					+ "https://www.googleapis.com/auth/calendar.events "
					+ "https://www.googleapis.com/auth/calendar.readonly";

	private final GoogleOAuthClient googleOAuthClient;
	private final JwtTokenProvider jwtTokenProvider;
	private final UserPublicService userPublicService;
	private final UserSessionRepository userSessionRepository;
	private final GoogleCalendarConnectionPublicService googleCalendarConnectionPublicService;

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
				.queryParam("scope", GOOGLE_LOGIN_SCOPE)
				.queryParam("access_type", "offline")
				.queryParam("include_granted_scopes", true)
				.queryParam("prompt", "select_account")
				.queryParam("state", state)
				.build()
				.encode()
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
		googleCalendarConnectionPublicService.saveAuthorizedConnection(
				user.id(),
				profile.email(),
				profile.googleAccessToken(),
				profile.googleRefreshToken(),
				profile.googleTokenExpiresIn()
		);
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

		UserSession session = userSessionRepository.findByRefreshTokenAndClientTypeForUpdate(
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

		UserSession session = userSessionRepository.findByRefreshTokenAndClientTypeForUpdate(
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

		userSessionRepository.upsertActiveSession(
				UUID.randomUUID(),
				user.id(),
				hashToken(refreshToken),
				clientType.name(),
				refreshTokenExpiresAt
		);

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
