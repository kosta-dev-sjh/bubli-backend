package com.bubli.personal.calendar.service;

import com.bubli.personal.calendar.dto.GoogleCalendarEventPayload;
import com.bubli.personal.calendar.dto.GoogleCalendarTokenResponse;
import com.bubli.personal.calendar.dto.GoogleCalendarUserInfoResponse;

import java.util.List;

public interface GoogleCalendarClient {

	GoogleCalendarTokenResponse exchangeCode(String code, String redirectUri);

	GoogleCalendarTokenResponse refresh(String refreshToken);

	GoogleCalendarUserInfoResponse fetchUserInfo(String accessToken);

	GoogleCalendarEventPayload createEvent(String accessToken, GoogleCalendarEventPayload payload);

	GoogleCalendarEventPayload updateEvent(String accessToken, String googleEventId, GoogleCalendarEventPayload payload);

	void deleteEvent(String accessToken, String googleEventId);

	List<GoogleCalendarEventPayload> getEvents(String accessToken, String timeMin, String timeMax);
}
