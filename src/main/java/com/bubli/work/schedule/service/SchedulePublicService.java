package com.bubli.work.schedule.service;

import com.bubli.work.schedule.dto.ScheduleResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SchedulePublicService {

	List<ScheduleResult> getSchedulesBetween(UUID userId, Instant from, Instant to);
}
