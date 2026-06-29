package com.bubli.memory.service;

import com.bubli.global.error.BusinessException;
import com.bubli.memory.dto.CreateDailySummaryDraftCommand;
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
import static org.mockito.Mockito.verify;
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

    @Test
    void publicServiceCreatesDraftWhenDailySummaryDoesNotExist() {
        UUID userId = UUID.randomUUID();
        LocalDate summaryDate = LocalDate.of(2026, 7, 1);
        DailySummaryRepository repository = mock(DailySummaryRepository.class);
        when(repository.findByUserIdAndSummaryDate(userId, summaryDate)).thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any(DailySummary.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = new DailySummaryPublicServiceImpl(repository)
                .upsertDraft(userId, new CreateDailySummaryDraftCommand(summaryDate, "{\"summary\":\"created\"}"));

        assertThat(response.summaryDate()).isEqualTo(summaryDate);
        assertThat(response.summaryJson()).contains("created");
        assertThat(response.status()).isEqualTo(SummaryStatus.DRAFT);
        verify(repository).save(org.mockito.ArgumentMatchers.any(DailySummary.class));
    }

    @Test
    void publicServiceUpdatesExistingDraftForSameDate() {
        UUID userId = UUID.randomUUID();
        LocalDate summaryDate = LocalDate.of(2026, 7, 1);
        DailySummary summary = DailySummary.draft(userId, summaryDate, "{\"summary\":\"old\"}");
        summary.approve();
        DailySummaryRepository repository = mock(DailySummaryRepository.class);
        when(repository.findByUserIdAndSummaryDate(userId, summaryDate)).thenReturn(Optional.of(summary));

        var response = new DailySummaryPublicServiceImpl(repository)
                .upsertDraft(userId, new CreateDailySummaryDraftCommand(summaryDate, "{\"summary\":\"new\"}"));

        assertThat(response.summaryJson()).contains("new");
        assertThat(response.status()).isEqualTo(SummaryStatus.DRAFT);
        assertThat(response.approvedAt()).isNull();
    }
}
