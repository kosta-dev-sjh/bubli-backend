package com.bubli.personal.calendar.repository;

import com.bubli.personal.calendar.entity.GoogleCalendarConnection;
import com.bubli.personal.calendar.type.GoogleCalendarConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface GoogleCalendarConnectionRepository extends JpaRepository<GoogleCalendarConnection, UUID> {

	Optional<GoogleCalendarConnection> findByUserId(UUID userId);

	Optional<GoogleCalendarConnection> findByUserIdAndStatus(UUID userId, GoogleCalendarConnectionStatus status);

	@Modifying
	@Query(value = """
			INSERT INTO google_calendar_connections (
			    id,
			    user_id,
			    google_account_email,
			    access_token,
			    refresh_token,
			    expires_at,
			    status,
			    created_at,
			    updated_at
			)
			VALUES (
			    :id,
			    :userId,
			    :googleAccountEmail,
			    :accessToken,
			    :refreshToken,
			    :expiresAt,
			    'ACTIVE',
			    CURRENT_TIMESTAMP,
			    CURRENT_TIMESTAMP
			)
			ON CONFLICT (user_id)
			DO UPDATE SET
			    google_account_email = EXCLUDED.google_account_email,
			    access_token = EXCLUDED.access_token,
			    refresh_token = COALESCE(NULLIF(EXCLUDED.refresh_token, ''), google_calendar_connections.refresh_token),
			    expires_at = EXCLUDED.expires_at,
			    status = 'ACTIVE',
			    updated_at = CURRENT_TIMESTAMP
			""", nativeQuery = true)
	int upsertActiveConnection(
			@Param("id") UUID id,
			@Param("userId") UUID userId,
			@Param("googleAccountEmail") String googleAccountEmail,
			@Param("accessToken") String accessToken,
			@Param("refreshToken") String refreshToken,
			@Param("expiresAt") Instant expiresAt
	);
}
