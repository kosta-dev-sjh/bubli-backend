package com.bubli.personal.calendar.service;

import com.bubli.personal.calendar.entity.GoogleCalendarDeleteRequest;
import com.bubli.personal.calendar.repository.GoogleCalendarDeleteRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoogleCalendarDeleteRequestService {

	private final GoogleCalendarDeleteRequestRepository repository;

	@Transactional
	public void rememberFailedAttempt(UUID userId, String googleEventId) {
		rememberFailedAttempt(userId, "primary", googleEventId);
	}

	@Transactional
	public void rememberFailedAttempt(UUID userId, String googleCalendarId, String googleEventId) {
		String normalizedCalendarId = normalizeCalendarId(googleCalendarId);
		String normalizedEventId = normalize(googleEventId);
		if (normalizedEventId == null) {
			return;
		}
		GoogleCalendarDeleteRequest request = repository
				.findByUserIdAndGoogleCalendarIdAndGoogleEventId(userId, normalizedCalendarId, normalizedEventId)
				.orElseGet(() -> GoogleCalendarDeleteRequest.create(userId, normalizedCalendarId, normalizedEventId));
		request.recordAttempt();
		repository.save(request);
	}

	@Transactional(readOnly = true)
	public Set<String> findPendingGoogleEventIds(UUID userId, Collection<String> googleEventIds) {
		return findPendingGoogleEventIds(userId, "primary", googleEventIds);
	}

	@Transactional(readOnly = true)
	public Set<String> findPendingGoogleEventIds(
			UUID userId,
			String googleCalendarId,
			Collection<String> googleEventIds
	) {
		List<String> ids = normalizeAll(googleEventIds);
		if (ids.isEmpty()) {
			return Set.of();
		}
		return repository.findByUserIdAndGoogleCalendarIdAndGoogleEventIdIn(
						userId,
						normalizeCalendarId(googleCalendarId),
						ids
				)
				.stream()
				.map(GoogleCalendarDeleteRequest::getGoogleEventId)
				.collect(Collectors.toSet());
	}

	@Transactional
	public void markSucceeded(UUID userId, String googleEventId) {
		markSucceeded(userId, "primary", googleEventId);
	}

	@Transactional
	public void markSucceeded(UUID userId, String googleCalendarId, String googleEventId) {
		String normalizedEventId = normalize(googleEventId);
		if (normalizedEventId == null) {
			return;
		}
		markSucceeded(userId, googleCalendarId, List.of(normalizedEventId));
	}

	@Transactional
	public void markSucceeded(UUID userId, Collection<String> googleEventIds) {
		markSucceeded(userId, "primary", googleEventIds);
	}

	@Transactional
	public void markSucceeded(UUID userId, String googleCalendarId, Collection<String> googleEventIds) {
		List<String> ids = normalizeAll(googleEventIds);
		if (ids.isEmpty()) {
			return;
		}
		repository.deleteByUserIdAndGoogleCalendarIdAndGoogleEventIdIn(
				userId,
				normalizeCalendarId(googleCalendarId),
				ids
		);
	}

	private List<String> normalizeAll(Collection<String> googleEventIds) {
		if (googleEventIds == null || googleEventIds.isEmpty()) {
			return List.of();
		}
		return googleEventIds.stream()
				.map(this::normalize)
				.filter(id -> id != null)
				.distinct()
				.toList();
	}

	private String normalize(String googleEventId) {
		if (googleEventId == null || googleEventId.isBlank()) {
			return null;
		}
		return googleEventId.trim();
	}

	private String normalizeCalendarId(String googleCalendarId) {
		return googleCalendarId == null || googleCalendarId.isBlank() ? "primary" : googleCalendarId.trim();
	}
}
