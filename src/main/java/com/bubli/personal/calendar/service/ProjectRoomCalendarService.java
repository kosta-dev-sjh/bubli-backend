package com.bubli.personal.calendar.service;

import com.bubli.personal.calendar.dto.RoomCalendarResponse;
import com.bubli.personal.calendar.entity.GoogleCalendarConnection;
import com.bubli.personal.calendar.entity.ProjectRoomGoogleCalendar;
import com.bubli.personal.calendar.repository.ProjectRoomGoogleCalendarRepository;
import com.bubli.project.dto.ProjectRoomResult;
import com.bubli.project.service.ProjectRoomPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectRoomCalendarService {

	private final ProjectRoomGoogleCalendarRepository roomCalendarRepository;
	private final GoogleCalendarConnectionService connectionService;
	private final GoogleCalendarClient googleCalendarClient;
	private final ProjectRoomPublicService projectRoomPublicService;

	/**
	 * 프로젝트 룸 전용 Google Calendar 매핑을 반환한다.
	 * 매핑이 없고 사용자의 Google 연동이 활성 상태이면 룸 이름으로 캘린더를 생성해 매핑을 저장한다.
	 * 연동이 없으면 빈 값을 반환해 일정이 로컬 전용으로 유지되게 한다.
	 */
	@Transactional
	public Optional<ProjectRoomGoogleCalendar> ensureRoomCalendar(UUID userId, UUID roomId) {
		Optional<ProjectRoomGoogleCalendar> existing = roomCalendarRepository.findByUserIdAndRoomId(userId, roomId);
		if (existing.isPresent()) {
			return existing;
		}
		Optional<GoogleCalendarConnection> connection = connectionService.getActiveConnectionWithFreshToken(userId);
		if (connection.isEmpty()) {
			return Optional.empty();
		}
		ProjectRoomResult room = projectRoomPublicService.getProjectRoom(userId, roomId);
		String googleCalendarId = googleCalendarClient.insertCalendar(connection.get().getAccessToken(), room.name());
		if (googleCalendarId == null || googleCalendarId.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.of(roomCalendarRepository.save(
					ProjectRoomGoogleCalendar.create(userId, roomId, googleCalendarId, room.name())
			));
		} catch (DataIntegrityViolationException exception) {
			// 동시 요청으로 이미 매핑이 저장된 경우 기존 매핑을 사용한다.
			return roomCalendarRepository.findByUserIdAndRoomId(userId, roomId);
		}
	}

	@Transactional(readOnly = true)
	public Map<UUID, String> findGoogleCalendarIds(UUID userId, Collection<UUID> roomIds) {
		if (roomIds == null || roomIds.isEmpty()) {
			return Map.of();
		}
		Map<UUID, String> calendarIds = new LinkedHashMap<>();
		for (ProjectRoomGoogleCalendar mapping : roomCalendarRepository.findByUserIdAndRoomIdIn(userId, roomIds)) {
			calendarIds.put(mapping.getRoomId(), mapping.getGoogleCalendarId());
		}
		return calendarIds;
	}

	@Transactional
	public RoomCalendarResponse getRoomCalendar(UUID userId, UUID roomId) {
		ProjectRoomResult room = projectRoomPublicService.getProjectRoom(userId, roomId);
		boolean connected = connectionService.hasActiveConnection(userId);
		Optional<ProjectRoomGoogleCalendar> mapping;
		if (connected) {
			try {
				mapping = ensureRoomCalendar(userId, roomId);
			} catch (RuntimeException exception) {
				// 기존 연동 토큰에 캘린더 생성 권한(scope)이 없으면 Google이 거부한다.
				// 500 대신 재동의 필요 플래그를 내려 프론트가 재연결을 안내하게 한다.
				return RoomCalendarResponse.reconsentRequired(room.name());
			}
		} else {
			mapping = roomCalendarRepository.findByUserIdAndRoomId(userId, roomId);
		}
		return mapping
				.map(found -> RoomCalendarResponse.of(
						found.getGoogleCalendarId(),
						found.getCalendarName() == null ? room.name() : found.getCalendarName(),
						connected
				))
				.orElseGet(() -> RoomCalendarResponse.of(null, room.name(), connected));
	}
}
