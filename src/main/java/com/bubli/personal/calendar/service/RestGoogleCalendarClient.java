package com.bubli.personal.calendar.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.personal.calendar.dto.GoogleCalendarEventPayload;
import com.bubli.personal.calendar.dto.GoogleCalendarListEntry;
import com.bubli.personal.calendar.dto.GoogleCalendarTokenResponse;
import com.bubli.personal.calendar.dto.GoogleCalendarUserInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
	private static final String EVENTS_PATH = "/calendar/v3/calendars/{calendarId}/events";
	private static final String CALENDARS_PATH = "/calendar/v3/calendars";
	private static final String CALENDAR_LIST_PATH = "/calendar/v3/users/me/calendarList";
	private static final String DEFAULT_TIME_ZONE = "Asia/Seoul";

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
	public String insertCalendar(String accessToken, String summary) {
		try {
			GoogleCalendarResource created = restClient.post()
					.uri(uriBuilder -> googleCalendarUri(uriBuilder)
							.path(CALENDARS_PATH)
							.build())
					.headers(headers -> headers.setBearerAuth(accessToken))
					.contentType(MediaType.APPLICATION_JSON)
					.body(new GoogleCalendarInsertBody(summary, DEFAULT_TIME_ZONE))
					.retrieve()
					.body(GoogleCalendarResource.class);
			return created == null ? null : created.id();
		} catch (RestClientException exception) {
			throw calendarException(exception);
		}
	}

	@Override
	public GoogleCalendarEventPayload createEvent(String accessToken, GoogleCalendarEventPayload payload) {
		return createEvent(accessToken, "primary", payload);
	}

	@Override
	public GoogleCalendarEventPayload createEvent(String accessToken, String calendarId, GoogleCalendarEventPayload payload) {
		try {
			return restClient.post()
					.uri(uriBuilder -> googleCalendarUri(uriBuilder)
							.path(EVENTS_PATH)
							.build(normalizeCalendarId(calendarId)))
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
		return updateEvent(accessToken, "primary", googleEventId, payload);
	}

	@Override
	public GoogleCalendarEventPayload updateEvent(
			String accessToken,
			String calendarId,
			String googleEventId,
			GoogleCalendarEventPayload payload
	) {
		try {
			return restClient.patch()
					.uri(uriBuilder -> googleCalendarUri(uriBuilder)
							.path(EVENTS_PATH)
							.path("/{eventId}")
							.build(normalizeCalendarId(calendarId), googleEventId))
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
		deleteEvent(accessToken, "primary", googleEventId);
	}

	@Override
	public void deleteEvent(String accessToken, String calendarId, String googleEventId) {
		try {
			restClient.delete()
					.uri(uriBuilder -> googleCalendarUri(uriBuilder)
							.path(EVENTS_PATH)
							.path("/{eventId}")
							.build(normalizeCalendarId(calendarId), googleEventId))
					.headers(headers -> headers.setBearerAuth(accessToken))
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientResponseException exception) {
			if (exception.getStatusCode() == HttpStatus.NOT_FOUND || exception.getStatusCode() == HttpStatus.GONE) {
				return;
			}
			throw calendarException(exception);
		} catch (RestClientException exception) {
			throw calendarException(exception);
		}
	}

	@Override
	public List<GoogleCalendarListEntry> getCalendars(String accessToken) {
		try {
			GoogleCalendarListResponse response = restClient.get()
					.uri(uriBuilder -> googleCalendarUri(uriBuilder)
							.path(CALENDAR_LIST_PATH)
							.queryParam("minAccessRole", "reader")
							.build())
					.headers(headers -> headers.setBearerAuth(accessToken))
					.retrieve()
					.body(GoogleCalendarListResponse.class);
			return response == null || response.items() == null ? List.of() : response.items();
		} catch (RestClientException exception) {
			throw calendarException(exception);
		}
	}

	@Override
	public List<GoogleCalendarEventPayload> getEvents(String accessToken, String timeMin, String timeMax) {
		return getEvents(accessToken, "primary", timeMin, timeMax);
	}

	@Override
	public List<GoogleCalendarEventPayload> getEvents(
			String accessToken,
			String calendarId,
			String timeMin,
			String timeMax
	) {
		try {
			GoogleCalendarEventsResponse response = restClient.get()
					.uri(uriBuilder -> googleCalendarUri(uriBuilder)
							.path(EVENTS_PATH)
							.queryParam("singleEvents", true)
							.queryParam("showDeleted", true)
							.queryParam("orderBy", "startTime")
							.queryParam("timeMin", timeMin)
							.queryParam("timeMax", timeMax)
							.build(normalizeCalendarId(calendarId)))
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

	private org.springframework.web.util.UriBuilder googleCalendarUri(org.springframework.web.util.UriBuilder uriBuilder) {
		return uriBuilder
				.scheme("https")
				.host("www.googleapis.com");
	}

	private String normalizeCalendarId(String calendarId) {
		return calendarId == null || calendarId.isBlank() ? "primary" : calendarId;
	}

	private record GoogleCalendarInsertBody(
			String summary,
			String timeZone
	) {
	}

	private record GoogleCalendarResource(
			String id,
			String summary
	) {
	}

	private record GoogleCalendarListResponse(
			List<GoogleCalendarListEntry> items
	) {
	}

	private record GoogleCalendarEventsResponse(
			List<GoogleCalendarEventPayload> items
	) {
	}
}
