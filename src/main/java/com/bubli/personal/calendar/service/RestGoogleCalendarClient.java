package com.bubli.personal.calendar.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.personal.calendar.dto.GoogleCalendarEventPayload;
import com.bubli.personal.calendar.dto.GoogleCalendarTokenResponse;
import com.bubli.personal.calendar.dto.GoogleCalendarUserInfoResponse;
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
import java.util.List;

@Component
@Slf4j
public class RestGoogleCalendarClient implements GoogleCalendarClient {

	private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
	private static final String USERINFO_URI = "https://openidconnect.googleapis.com/v1/userinfo";
	private static final String EVENTS_URI = "https://www.googleapis.com/calendar/v3/calendars/primary/events";

	private final RestClient restClient;

	@Value("${google.calendar.client-id:${google.oauth.client-id:}}")
	private String clientId;

	@Value("${google.calendar.client-secret:${google.oauth.client-secret:}}")
	private String clientSecret;

	public RestGoogleCalendarClient(RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder.build();
	}

	@Override
	public GoogleCalendarTokenResponse exchangeCode(String code, String redirectUri) {
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("code", UriUtils.decode(code, StandardCharsets.UTF_8));
		body.add("client_id", clientId);
		body.add("client_secret", clientSecret);
		body.add("redirect_uri", redirectUri);
		body.add("grant_type", "authorization_code");
		return postToken(body);
	}

	@Override
	public GoogleCalendarTokenResponse refresh(String refreshToken) {
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("refresh_token", refreshToken);
		body.add("client_id", clientId);
		body.add("client_secret", clientSecret);
		body.add("grant_type", "refresh_token");
		return postToken(body);
	}

	@Override
	public GoogleCalendarUserInfoResponse fetchUserInfo(String accessToken) {
		try {
			return restClient.get()
					.uri(USERINFO_URI)
					.headers(headers -> headers.setBearerAuth(accessToken))
					.retrieve()
					.body(GoogleCalendarUserInfoResponse.class);
		} catch (RestClientException exception) {
			throw calendarException(exception);
		}
	}

	@Override
	public GoogleCalendarEventPayload createEvent(String accessToken, GoogleCalendarEventPayload payload) {
		try {
			return restClient.post()
					.uri(EVENTS_URI)
					.headers(headers -> headers.setBearerAuth(accessToken))
					.contentType(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.body(GoogleCalendarEventPayload.class);
		} catch (RestClientException exception) {
			throw calendarException(exception);
		}
	}

	@Override
	public GoogleCalendarEventPayload updateEvent(String accessToken, String googleEventId, GoogleCalendarEventPayload payload) {
		try {
			return restClient.patch()
					.uri(EVENTS_URI + "/{eventId}", googleEventId)
					.headers(headers -> headers.setBearerAuth(accessToken))
					.contentType(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.body(GoogleCalendarEventPayload.class);
		} catch (RestClientException exception) {
			throw calendarException(exception);
		}
	}

	@Override
	public void deleteEvent(String accessToken, String googleEventId) {
		try {
			restClient.delete()
					.uri(EVENTS_URI + "/{eventId}", googleEventId)
					.headers(headers -> headers.setBearerAuth(accessToken))
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException exception) {
			throw calendarException(exception);
		}
	}

	@Override
	public List<GoogleCalendarEventPayload> getEvents(String accessToken, String timeMin, String timeMax) {
		try {
			GoogleCalendarEventsResponse response = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.scheme("https")
							.host("www.googleapis.com")
							.path("/calendar/v3/calendars/primary/events")
							.queryParam("singleEvents", true)
							.queryParam("orderBy", "startTime")
							.queryParam("timeMin", timeMin)
							.queryParam("timeMax", timeMax)
							.build())
					.headers(headers -> headers.setBearerAuth(accessToken))
					.retrieve()
					.body(GoogleCalendarEventsResponse.class);
			return response == null || response.items() == null ? List.of() : response.items();
		} catch (RestClientException exception) {
			throw calendarException(exception);
		}
	}

	private GoogleCalendarTokenResponse postToken(MultiValueMap<String, String> body) {
		try {
			return restClient.post()
					.uri(TOKEN_URI)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(body)
					.retrieve()
					.body(GoogleCalendarTokenResponse.class);
		} catch (RestClientException exception) {
			throw calendarException(exception);
		}
	}

	private BusinessException calendarException(RestClientException exception) {
		if (exception instanceof RestClientResponseException responseException) {
			log.warn(
					"Google Calendar request failed. status={}, response={}",
					responseException.getStatusCode(),
					responseException.getResponseBodyAsString()
			);
		} else {
			log.warn("Google Calendar request failed.", exception);
		}
		return new BusinessException(ErrorCode.CALENDAR_502_001);
	}

	private record GoogleCalendarEventsResponse(
			List<GoogleCalendarEventPayload> items
	) {
	}
}
