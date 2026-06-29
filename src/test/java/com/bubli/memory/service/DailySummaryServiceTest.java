package com.bubli.memory.service;

import com.bubli.global.error.BusinessException;
import com.bubli.memory.dto.DailySummaryAction;
import com.bubli.memory.dto.DailySummaryPatchCommand;
import com.bubli.memory.entity.DailySummary;
import com.bubli.memory.repository.DailySummaryRepository;
import com.bubli.memory.type.SummaryStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DailySummaryServiceTest {

    @Test
    void approvesOwnDailySummary() {
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        DailySummary summary = DailySummary.draft(userId, LocalDate.of(2026, 7, 1), "{\"items\":[]}");
        DailySummaryRepository repository = mock(DailySummaryRepository.class);
        when(repository.findByIdAndUserId(summaryId, userId)).thenReturn(Optional.of(summary));

        var response = new DailySummaryService(repository)
                .patch(userId, summaryId, new DailySummaryPatchCommand(DailySummaryAction.APPROVE, null));

        assertThat(response.status()).isEqualTo(SummaryStatus.APPROVED);
        assertThat(response.approvedAt()).isNotNull();
    }

    @Test
    void editRequiresSummaryJson() {
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        DailySummary summary = DailySummary.draft(userId, LocalDate.of(2026, 7, 1), "{\"items\":[]}");
        DailySummaryRepository repository = mock(DailySummaryRepository.class);
        when(repository.findByIdAndUserId(summaryId, userId)).thenReturn(Optional.of(summary));

        assertThatThrownBy(() -> new DailySummaryService(repository)
                .patch(userId, summaryId, new DailySummaryPatchCommand(DailySummaryAction.EDIT, " ")))
                .isInstanceOf(BusinessException.class);
    }
}
