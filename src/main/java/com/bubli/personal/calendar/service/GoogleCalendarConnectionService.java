package com.bubli.personal.calendar.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.personal.calendar.dto.GoogleCalendarConnectResponse;
import com.bubli.personal.calendar.dto.GoogleCalendarConnectionResponse;
import com.bubli.personal.calendar.dto.GoogleCalendarTokenResponse;
import com.bubli.personal.calendar.dto.GoogleCalendarUserInfoResponse;
import com.bubli.personal.calendar.entity.GoogleCalendarConnection;
import com.bubli.personal.calendar.repository.GoogleCalendarConnectionRepository;
import com.bubli.personal.calendar.type.GoogleCalendarConnectionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleCalendarConnectionService {

	private static final String GOOGLE_AUTHORIZE_URI = "https://accounts.google.com/o/oauth2/v2/auth";
	private static final String CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar.events";

	private final GoogleCalendarConnectionRepository connectionRepository;
	private final GoogleCalendarClient googleCalendarClient;

	@Value("${google.calendar.client-id:${google.oauth.client-id:}}")
	private String clientId;

	@Value("${google.calendar.redirect-uri:http://localhost:3000/calendar/google/callback}")
	private String defaultRedirectUri;

	@Transactional(readOnly = true)
	public GoogleCalendarConnectResponse createConnectUrl(UUID userId, String redirectUri) {
		String callbackUri = hasText(redirectUri) ? redirectUri : defaultRedirectUri;
		String authorizeUrl = UriComponentsBuilder.fromUriString(GOOGLE_AUTHORIZE_URI)
				.queryParam("client_id", clientId)
				.queryParam("redirect_uri", callbackUri)
				.queryParam("response_type", "code")
				.queryParam("scope", CALENDAR_SCOPE + " openid email")
				.queryParam("access_type", "offline")
				.queryParam("prompt", "consent")
				.queryParam("state", userId)
				.build()
				.toUriString();
		return new GoogleCalendarConnectResponse(authorizeUrl);
	}

	@Transactional
	public GoogleCalendarConnectionResponse connect(UUID userId, String code, String redirectUri) {
		String callbackUri = hasText(redirectUri) ? redirectUri : defaultRedirectUri;
		GoogleCalendarTokenResponse token = googleCalendarClient.exchangeCode(code, callbackUri);
		if (token == null || !hasText(token.accessToken())) {
			throw new BusinessException(ErrorCode.CALENDAR_400_001);
		}
		GoogleCalendarUserInfoResponse userInfo = googleCalendarClient.fetchUserInfo(token.accessToken());
		Instant expiresAt = expiresAt(token);
		GoogleCalendarConnection connection = connectionRepository.findByUserId(userId)
				.orElseGet(() -> GoogleCalendarConnection.create(
						userId,
						userInfo == null ? null : userInfo.email(),
						token.accessToken(),
						token.refreshToken(),
						expiresAt
				));
		connection.updateTokens(
				userInfo == null ? null : userInfo.email(),
				token.accessToken(),
				token.refreshToken(),
				expiresAt
		);
		return GoogleCalendarConnectionResponse.from(connectionRepository.save(connection));
	}

	@Transactional(readOnly = true)
	public Optional<GoogleCalendarConnectionResponse> getConnection(UUID userId) {
		return connectionRepository.findByUserId(userId)
				.map(GoogleCalendarConnectionResponse::from);
	}

	@Transactional(readOnly = true)
	public boolean hasActiveConnection(UUID userId) {
		return connectionRepository.findByUserIdAndStatus(userId, GoogleCalendarConnectionStatus.ACTIVE).isPresent();
	}

	@Transactional
	public void disconnect(UUID userId) {
		GoogleCalendarConnection connection = connectionRepository.findByUserIdAndStatus(
						userId,
						GoogleCalendarConnectionStatus.ACTIVE
				)
				.orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_404_001));
		connection.revoke();
	}

	@Transactional
	public Optional<GoogleCalendarConnection> getActiveConnectionWithFreshToken(UUID userId) {
		Optional<GoogleCalendarConnection> maybeConnection = connectionRepository.findByUserIdAndStatus(
				userId,
				GoogleCalendarConnectionStatus.ACTIVE
		);
		if (maybeConnection.isEmpty()) {
			return Optional.empty();
		}
		GoogleCalendarConnection connection = maybeConnection.get();
		if (connection.shouldRefresh(Instant.now())) {
			if (!hasText(connection.getRefreshToken())) {
				connection.revoke();
				return Optional.empty();
			}
			GoogleCalendarTokenResponse token = googleCalendarClient.refresh(connection.getRefreshToken());
			if (token == null || !hasText(token.accessToken())) {
				connection.revoke();
				return Optional.empty();
			}
			connection.refreshAccessToken(token.accessToken(), expiresAt(token));
		}
		return Optional.of(connection);
	}

	private Instant expiresAt(GoogleCalendarTokenResponse token) {
		long expiresIn = token.expiresIn() == null ? 3600L : token.expiresIn();
		return Instant.now().plusSeconds(expiresIn);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
