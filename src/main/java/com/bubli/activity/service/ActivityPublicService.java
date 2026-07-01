package com.bubli.activity.service;

import com.bubli.activity.dto.ActivityLogResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ActivityPublicService {

	List<ActivityLogResult> getActivityContextBetween(UUID userId, Instant from, Instant to, int limit);
}
