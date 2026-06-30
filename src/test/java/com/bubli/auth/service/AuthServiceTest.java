package com.bubli.auth.service;

import com.bubli.auth.dto.AuthTokenResponse;
import com.bubli.auth.dto.GoogleAuthorizeCommand;
import com.bubli.auth.dto.GoogleCallbackCommand;
import com.bubli.auth.dto.GoogleUserProfile;
import com.bubli.auth.dto.RefreshTokenCommand;
import com.bubli.auth.entity.UserSession;
import com.bubli.auth.repository.UserSessionRepository;
import com.bubli.auth.type.ClientType;
import com.bubli.global.error.BusinessException;
import com.bubli.global.security.JwtTokenProvider;
import com.bubli.personal.calendar.service.GoogleCalendarConnectionPublicService;
import com.bubli.user.dto.UpsertGoogleUserCommand;
import com.bubli.user.dto.UserResult;
import com.bubli.user.entity.User;
import com.bubli.user.service.UserPublicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	GoogleOAuthClient googleOAuthClient;

	@Mock
	UserPublicService userPublicService;

	@Mock
	UserSessionRepository userSessionRepository;

	@Mock
	GoogleCalendarConnectionPublicService googleCalendarConnectionPublicService;

	JwtTokenProvider jwtTokenProvider;

	AuthService authService;

	@BeforeEach
	void setUp() {
		jwtTokenProvider = new JwtTokenProvider();
		ReflectionTestUtils.setField(jwtTokenProvider, "secret", "test-secret-key-must-be-at-least-32-bytes");
		ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpireMs", 3_600_000L);
		ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpireMs", 1_209_600_000L);
		ReflectionTestUtils.invokeMethod(jwtTokenProvider, "init");

		authService = new AuthService(
				googleOAuthClient,
				jwtTokenProvider,
				userPublicService,
				userSessionRepository,
				googleCalendarConnectionPublicService
		);
		ReflectionTestUtils.setField(authService, "googleClientId", "google-client-id");
		ReflectionTestUtils.setField(authService, "defaultRedirectUri", "http://localhost:3000/auth/callback");
	}

	@Test
	void createGoogleAuthorizeUrlBuildsOAuthUrl() {
		String authorizeUrl = authService.createGoogleAuthorizeUrl(new GoogleAuthorizeCommand(
				"http://localhost:3000/callback",
				ClientType.WEB,
				"state-value"
		)).authorizeUrl();

		assertThat(authorizeUrl).contains("https://accounts.google.com/o/oauth2/v2/auth");
		assertThat(authorizeUrl).contains("client_id=google-client-id");
		assertThat(authorizeUrl).contains("redirect_uri=http://localhost:3000/callback");
		assertThat(authorizeUrl).contains("response_type=code");
		assertThat(authorizeUrl).contains("state=state-value");
	}

	@Test
	void handleGoogleCallbackCreatesUserAndSessionWhenFirstLogin() {
		GoogleCallbackCommand command = new GoogleCallbackCommand(
				"oauth-code",
				"http://localhost:3000/auth/callback",
				ClientType.TAURI
		);
		GoogleUserProfile profile = new GoogleUserProfile(
				"google-sub",
				"미연",
				"https://cdn.example/avatar.png",
				"ko",
				"miyeon@example.com",
				"google-access-token",
				"google-refresh-token",
				3600L
		);
		UUID userId = UUID.randomUUID();
		UserResult user = new UserResult(
				userId,
				"bubli-id",
				"미연",
				"https://cdn.example/avatar.png",
				"ko",
				null
		);
		given(googleOAuthClient.fetchUserProfile(command)).willReturn(profile);
		given(userPublicService.upsertGoogleUser(any(UpsertGoogleUserCommand.class))).willReturn(user);
		given(userSessionRepository.findByUserIdAndClientType(userId, ClientType.TAURI)).willReturn(Optional.empty());
		given(userSessionRepository.save(any(UserSession.class))).willAnswer(invocation -> invocation.getArgument(0));

		AuthTokenResponse response = authService.handleGoogleCallback(command);

		assertThat(response.accessToken()).isNotBlank();
		assertThat(response.refreshToken()).isNotBlank();
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.user().id()).isEqualTo(userId);
		assertThat(response.user().name()).isEqualTo("미연");

		ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
		verify(userSessionRepository).save(sessionCaptor.capture());
		assertThat(sessionCaptor.getValue().getRefreshToken()).isNotEqualTo(response.refreshToken());
		assertThat(sessionCaptor.getValue().getUserId()).isEqualTo(userId);
		verify(googleCalendarConnectionPublicService).saveAuthorizedConnection(
				userId,
				"miyeon@example.com",
				"google-access-token",
				"google-refresh-token",
				3600L
		);
	}

	@Test
	void refreshRotatesRefreshToken() {
		UUID userId = UUID.randomUUID();
		User user = User.createGoogleUser("google-sub", "bubli-id", "미연", null, "ko", "Asia/Seoul");
		ReflectionTestUtils.setField(user, "id", userId);
		AuthTokenResponse loginResponse = loginExistingUser(user);
		UserSession session = captureSavedSession();
		String oldRefreshTokenHash = session.getRefreshToken();

		given(userSessionRepository.findByRefreshTokenAndClientType(oldRefreshTokenHash, ClientType.TAURI))
				.willReturn(Optional.of(session));
		given(userPublicService.getUser(userId)).willReturn(new UserResult(
				userId,
				user.getBubliId(),
				user.getName(),
				user.getAvatarUrl(),
				user.getLocale(),
				user.getTimezone()
		));

		AuthTokenResponse refreshResponse = authService.refresh(new RefreshTokenCommand(
				loginResponse.refreshToken(),
				ClientType.TAURI
		));

		assertThat(refreshResponse.accessToken()).isNotBlank();
		assertThat(refreshResponse.refreshToken()).isNotEqualTo(loginResponse.refreshToken());
		assertThat(session.getRefreshToken()).isNotEqualTo(oldRefreshTokenHash);
	}

	@Test
	void refreshThrowsWhenSessionDoesNotMatchToken() {
		UUID userId = UUID.randomUUID();
		User user = User.createGoogleUser("google-sub", "bubli-id", "미연", null, "ko", "Asia/Seoul");
		ReflectionTestUtils.setField(user, "id", userId);
		AuthTokenResponse loginResponse = loginExistingUser(user);
		UserSession session = captureSavedSession();
		given(userSessionRepository.findByRefreshTokenAndClientType(session.getRefreshToken(), ClientType.TAURI))
				.willReturn(Optional.empty());

		assertThatThrownBy(() -> authService.refresh(new RefreshTokenCommand(
				loginResponse.refreshToken(),
				ClientType.TAURI
		))).isInstanceOf(BusinessException.class);
	}

	@Test
	void logoutRevokesOnlyMatchedRefreshTokenSession() {
		UUID userId = UUID.randomUUID();
		User user = User.createGoogleUser("google-sub", "bubli-id", "미연", null, "ko", "Asia/Seoul");
		ReflectionTestUtils.setField(user, "id", userId);
		AuthTokenResponse loginResponse = loginExistingUser(user);
		UserSession tauriSession = UserSession.create(
				userId,
				captureSavedSession().getRefreshToken(),
				ClientType.TAURI,
				Instant.now().plusSeconds(60)
		);
		UserSession webSession = UserSession.create(
				userId,
				"hash-2",
				ClientType.WEB,
				Instant.now().plusSeconds(60)
		);
		given(userSessionRepository.findByRefreshTokenAndClientType(tauriSession.getRefreshToken(), ClientType.TAURI))
				.willReturn(Optional.of(tauriSession));

		authService.logout(userId, new RefreshTokenCommand(loginResponse.refreshToken(), ClientType.TAURI));

		assertThat(tauriSession.isActiveAt(Instant.now())).isFalse();
		assertThat(webSession.isActiveAt(Instant.now())).isTrue();
	}

	private AuthTokenResponse loginExistingUser(User user) {
		GoogleCallbackCommand command = new GoogleCallbackCommand(
				"oauth-code",
				"http://localhost:3000/auth/callback",
				ClientType.TAURI
		);
		GoogleUserProfile profile = new GoogleUserProfile(
				user.getGoogleSub(),
				user.getName(),
				user.getAvatarUrl(),
				user.getLocale(),
				null,
				null,
				null,
				null
		);
		given(googleOAuthClient.fetchUserProfile(command)).willReturn(profile);
		given(userPublicService.upsertGoogleUser(any(UpsertGoogleUserCommand.class))).willReturn(new UserResult(
				user.getId(),
				user.getBubliId(),
				user.getName(),
				user.getAvatarUrl(),
				user.getLocale(),
				user.getTimezone()
		));
		given(userSessionRepository.findByUserIdAndClientType(user.getId(), ClientType.TAURI)).willReturn(Optional.empty());
		given(userSessionRepository.save(any(UserSession.class))).willAnswer(invocation -> invocation.getArgument(0));

		return authService.handleGoogleCallback(command);
	}

	private UserSession captureSavedSession() {
		ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
		verify(userSessionRepository).save(sessionCaptor.capture());
		return sessionCaptor.getValue();
	}
}
