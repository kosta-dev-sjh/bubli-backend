package com.bubli.personal.calendar.service;

import java.util.UUID;

public interface GoogleCalendarConnectionPublicService {

	void saveAuthorizedConnection(
			UUID userId,
			String googleAccountEmail,
			String accessToken,
			String refreshToken,
			Long expiresIn
	);
}
