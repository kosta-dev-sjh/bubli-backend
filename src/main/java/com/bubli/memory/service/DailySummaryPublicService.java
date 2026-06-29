package com.bubli.memory.service;

import com.bubli.memory.dto.CreateDailySummaryDraftCommand;
import com.bubli.memory.dto.DailySummaryResponse;

import java.util.UUID;

public interface DailySummaryPublicService {

	DailySummaryResponse upsertDraft(UUID userId, CreateDailySummaryDraftCommand command);
}
