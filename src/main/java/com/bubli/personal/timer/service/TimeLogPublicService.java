package com.bubli.personal.timer.service;

import com.bubli.personal.timer.dto.TimeLogResult;

import java.util.Optional;
import java.util.UUID;

public interface TimeLogPublicService {

	Optional<TimeLogResult> getRunningTimer(UUID userId);
}
