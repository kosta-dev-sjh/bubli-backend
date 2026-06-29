package com.bubli.memory.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.memory.dto.CreateDailySummaryDraftCommand;
import com.bubli.memory.dto.DailySummaryResponse;
import com.bubli.memory.entity.DailySummary;
import com.bubli.memory.repository.DailySummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DailySummaryPublicServiceImpl implements DailySummaryPublicService {

	private final DailySummaryRepository dailySummaryRepository;

	@Override
	@Transactional
	public DailySummaryResponse upsertDraft(UUID userId, CreateDailySummaryDraftCommand command) {
		if (command == null || command.summaryDate() == null
				|| command.summaryJson() == null || command.summaryJson().isBlank()) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}
		DailySummary summary = dailySummaryRepository
				.findByUserIdAndSummaryDate(userId, command.summaryDate())
				.map(existing -> {
					existing.edit(command.summaryJson().trim());
					return existing;
				})
				.orElseGet(() -> dailySummaryRepository.save(DailySummary.draft(
						userId,
						command.summaryDate(),
						command.summaryJson().trim()
				)));
		return DailySummaryResponse.from(summary);
	}
}
