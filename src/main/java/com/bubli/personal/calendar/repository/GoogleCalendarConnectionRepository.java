package com.bubli.personal.calendar.repository;

import com.bubli.personal.calendar.entity.GoogleCalendarConnection;
import com.bubli.personal.calendar.type.GoogleCalendarConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GoogleCalendarConnectionRepository extends JpaRepository<GoogleCalendarConnection, UUID> {

	Optional<GoogleCalendarConnection> findByUserId(UUID userId);

	Optional<GoogleCalendarConnection> findByUserIdAndStatus(UUID userId, GoogleCalendarConnectionStatus status);
}
