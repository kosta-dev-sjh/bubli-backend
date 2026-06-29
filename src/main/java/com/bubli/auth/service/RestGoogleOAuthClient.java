package com.bubli.auth.service;

import com.bubli.auth.dto.GoogleCallbackCommand;
import com.bubli.auth.dto.GoogleUserProfile;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class RestGoogleOAuthClient implements GoogleOAuthClient {

	private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
	private static final String USERINFO_URI = "https://openidconnect.googleapis.com/v1/userinfo";

	private final RestClient restClient;

	@Value("${google.oauth.client-id:${google.calendar.client-id:}}")
	private String clientId;

	@Value("${google.oauth.client-secret:${google.calendar.client-secret:}}")
	private String clientSecret;

	public RestGoogleOAuthClient(RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder.build();
	}

	@Override
	public GoogleUserProfile fetchUserProfile(GoogleCallbackCommand command) {
		try {
			GoogleTokenResponse tokenResponse = exchangeCode(command);
			if (tokenResponse == null || tokenResponse.accessToken() == null || tokenResponse.accessToken().isBlank()) {
				throw new BusinessException(ErrorCode.AUTH_401_004);
			}
			GoogleUserInfoResponse userInfo = fetchUserInfo(tokenResponse.accessToken());
			if (userInfo == null || userInfo.sub() == null || userInfo.sub().isBlank()) {
				throw new BusinessException(ErrorCode.AUTH_401_005);
			}
			return new GoogleUserProfile(
					userInfo.sub(),
					userInfo.name(),
					userInfo.picture(),
					userInfo.locale()
			);
		} catch (BusinessException e) {
			throw e;
		} catch (RestClientResponseException e) {
			log.warn("Google OAuth request failed. status={}, response={}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new BusinessException(ErrorCode.AUTH_401_004);
		} catch (RestClientException e) {
			log.warn("Google OAuth request failed.", e);
			throw new BusinessException(ErrorCode.AUTH_401_004);
		}
	}

	private GoogleTokenResponse exchangeCode(GoogleCallbackCommand command) {
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("code", UriUtils.decode(command.code(), StandardCharsets.UTF_8));
		body.add("client_id", clientId);
		body.add("client_secret", clientSecret);
		body.add("redirect_uri", command.redirectUri());
		body.add("grant_type", "authorization_code");

		return restClient.post()
				.uri(TOKEN_URI)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(body)
				.retrieve()
				.body(GoogleTokenResponse.class);
	}

	private GoogleUserInfoResponse fetchUserInfo(String accessToken) {
		return restClient.get()
				.uri(USERINFO_URI)
				.headers(headers -> headers.setBearerAuth(accessToken))
				.retrieve()
				.body(GoogleUserInfoResponse.class);
	}

	private record GoogleTokenResponse(
			@com.fasterxml.jackson.annotation.JsonProperty("access_token")
			String accessToken
	) {
	}

	private record GoogleUserInfoResponse(
			String sub,
			String name,
			String picture,
			String locale
	) {
	}
}
