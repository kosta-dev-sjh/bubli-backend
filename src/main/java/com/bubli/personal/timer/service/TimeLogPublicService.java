package com.bubli.personal.timer.service;

import com.bubli.personal.timer.dto.TimeLogActivityRow;
import com.bubli.personal.timer.dto.TimeLogResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeLogPublicService {

	Optional<TimeLogResult> getRunningTimer(UUID userId);

	void stopRunningRoomTimer(UUID userId, UUID roomId);

	List<TimeLogActivityRow> getActivityBetween(UUID userId, Instant from, Instant to);
}
