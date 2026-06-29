package com.bubli.memory.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.memory.dto.DailySummaryPatchCommand;
import com.bubli.memory.dto.DailySummaryResponse;
import com.bubli.memory.entity.DailySummary;
import com.bubli.memory.repository.DailySummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DailySummaryService {

    private final DailySummaryRepository dailySummaryRepository;

    @Transactional(readOnly = true)
    public PageResponse<DailySummaryResponse> getDailySummaries(UUID userId, Pageable pageable) {
        Page<DailySummaryResponse> page = dailySummaryRepository.findByUserIdOrderBySummaryDateDesc(userId, pageable)
                .map(DailySummaryResponse::from);
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }

    @Transactional
    public DailySummaryResponse patch(UUID userId, UUID summaryId, DailySummaryPatchCommand command) {
        DailySummary summary = dailySummaryRepository.findByIdAndUserId(summaryId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_404_001));
        switch (command.action()) {
            case APPROVE -> summary.approve();
            case EDIT -> {
                if (command.summaryJson() == null || command.summaryJson().isBlank()) {
                    throw new BusinessException(ErrorCode.COMMON_400_002);
                }
                summary.edit(command.summaryJson().trim());
            }
            case HOLD -> summary.hold();
        }
        return DailySummaryResponse.from(summary);
    }
}
