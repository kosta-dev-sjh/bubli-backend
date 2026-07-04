package com.bubli.personal.calendar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
		name = "project_room_google_calendars",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_project_room_google_calendars_user_room",
				columnNames = {"user_id", "room_id"}
		)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectRoomGoogleCalendar {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "room_id", nullable = false)
	private UUID roomId;

	@Column(name = "google_calendar_id", nullable = false, length = 255)
	private String googleCalendarId;

	@Column(name = "calendar_name", length = 255)
	private String calendarName;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public static ProjectRoomGoogleCalendar create(
			UUID userId,
			UUID roomId,
			String googleCalendarId,
			String calendarName
	) {
		ProjectRoomGoogleCalendar mapping = new ProjectRoomGoogleCalendar();
		mapping.userId = userId;
		mapping.roomId = roomId;
		mapping.googleCalendarId = googleCalendarId;
		mapping.calendarName = calendarName;
		return mapping;
	}

	@PrePersist
	private void onCreate() {
		this.createdAt = Instant.now();
	}
}
