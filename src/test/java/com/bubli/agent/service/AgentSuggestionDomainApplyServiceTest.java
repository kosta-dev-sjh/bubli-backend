package com.bubli.agent.service;

import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.memory.dto.CreateDailySummaryDraftCommand;
import com.bubli.memory.service.DailySummaryPublicService;
import com.bubli.work.schedule.dto.CreateScheduleCommand;
import com.bubli.work.schedule.service.SchedulePublicService;
import com.bubli.work.task.dto.CreateRoomTaskCommand;
import com.bubli.work.task.service.TaskPublicService;
import com.bubli.work.task.type.TaskStatus;
import com.bubli.work.wbs.dto.CreateWbsItemCommand;
import com.bubli.work.wbs.service.WbsItemPublicService;
import com.bubli.work.wbs.type.WbsStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;

class AgentSuggestionDomainApplyServiceTest {

    @Test
    void appliesTaskSuggestionToRoomTask() {
        TaskPublicService taskPublicService = mock(TaskPublicService.class);
        AgentSuggestionDomainApplyService service = service(taskPublicService, mock(WbsItemPublicService.class), mock(SchedulePublicService.class), mock(DailySummaryPublicService.class));
        UUID reviewerId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID wbsItemId = UUID.randomUUID();
        AgentSuggestion suggestion = suggestion(roomId, AgentSuggestionType.TASK, Map.of(
                "title", "API 구현",
                "description", "로그인 API 구현",
                "assigneeUserId", assigneeId.toString(),
                "wbsItemId", wbsItemId.toString(),
                "status", "TODO",
                "dueAt", "2026-07-01T00:00:00Z"
        ));

        service.applyApprovedSuggestion(reviewerId, suggestion);

        ArgumentCaptor<CreateRoomTaskCommand> commandCaptor = ArgumentCaptor.forClass(CreateRoomTaskCommand.class);
        verify(taskPublicService).createRoomTask(org.mockito.ArgumentMatchers.eq(reviewerId), org.mockito.ArgumentMatchers.eq(roomId), commandCaptor.capture());
        assertThat(commandCaptor.getValue().title()).isEqualTo("API 구현");
        assertThat(commandCaptor.getValue().status()).isEqualTo(TaskStatus.TODO);
        assertThat(commandCaptor.getValue().assigneeUserId()).isEqualTo(assigneeId);
        assertThat(commandCaptor.getValue().wbsItemId()).isEqualTo(wbsItemId);
    }

    @Test
    void appliesWbsSuggestionToWbsItem() {
        WbsItemPublicService wbsItemPublicService = mock(WbsItemPublicService.class);
        AgentSuggestionDomainApplyService service = service(mock(TaskPublicService.class), wbsItemPublicService, mock(SchedulePublicService.class), mock(DailySummaryPublicService.class));
        UUID reviewerId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        AgentSuggestion suggestion = suggestion(roomId, AgentSuggestionType.WBS, Map.of(
                "title", "요구사항 정리",
                "parentId", parentId.toString(),
                "orderNo", 3,
                "status", "IN_PROGRESS"
        ));

        service.applyApprovedSuggestion(reviewerId, suggestion);

        ArgumentCaptor<CreateWbsItemCommand> commandCaptor = ArgumentCaptor.forClass(CreateWbsItemCommand.class);
        verify(wbsItemPublicService).create(org.mockito.ArgumentMatchers.eq(reviewerId), org.mockito.ArgumentMatchers.eq(roomId), commandCaptor.capture());
        assertThat(commandCaptor.getValue().title()).isEqualTo("요구사항 정리");
        assertThat(commandCaptor.getValue().parentId()).isEqualTo(parentId);
        assertThat(commandCaptor.getValue().orderNo()).isEqualTo(3);
        assertThat(commandCaptor.getValue().status()).isEqualTo(WbsStatus.IN_PROGRESS);
    }

    @Test
    void appliesScheduleSuggestionToSchedule() {
        SchedulePublicService schedulePublicService = mock(SchedulePublicService.class);
        AgentSuggestionDomainApplyService service = service(mock(TaskPublicService.class), mock(WbsItemPublicService.class), schedulePublicService, mock(DailySummaryPublicService.class));
        UUID reviewerId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        Instant startsAt = Instant.parse("2026-07-01T01:00:00Z");
        Instant endsAt = Instant.parse("2026-07-01T02:00:00Z");
        AgentSuggestion suggestion = suggestion(roomId, AgentSuggestionType.SCHEDULE, Map.of(
                "title", "회의",
                "startsAt", startsAt.toString(),
                "endsAt", endsAt.toString(),
                "allDay", false
        ));

        service.applyApprovedSuggestion(reviewerId, suggestion);

        ArgumentCaptor<CreateScheduleCommand> commandCaptor = ArgumentCaptor.forClass(CreateScheduleCommand.class);
        verify(schedulePublicService).create(org.mockito.ArgumentMatchers.eq(reviewerId), commandCaptor.capture());
        assertThat(commandCaptor.getValue().roomId()).isEqualTo(roomId);
        assertThat(commandCaptor.getValue().title()).isEqualTo("회의");
        assertThat(commandCaptor.getValue().startsAt()).isEqualTo(startsAt);
        assertThat(commandCaptor.getValue().endsAt()).isEqualTo(endsAt);
    }

    @Test
    void appliesDailySummarySuggestionToDailySummaryDraft() {
        DailySummaryPublicService dailySummaryPublicService = mock(DailySummaryPublicService.class);
        AgentSuggestionDomainApplyService service = service(
                mock(TaskPublicService.class),
                mock(WbsItemPublicService.class),
                mock(SchedulePublicService.class),
                dailySummaryPublicService
        );
        UUID reviewerId = UUID.randomUUID();
        AgentSuggestion suggestion = suggestion(null, AgentSuggestionType.DAILY_SUMMARY, Map.of(
                "summaryDate", "2026-07-01",
                "summaryJson", "{\"summary\":\"오늘 작업 요약\"}"
        ));

        service.applyApprovedSuggestion(reviewerId, suggestion);

        ArgumentCaptor<CreateDailySummaryDraftCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateDailySummaryDraftCommand.class);
        verify(dailySummaryPublicService).upsertDraft(org.mockito.ArgumentMatchers.eq(reviewerId), commandCaptor.capture());
        assertThat(commandCaptor.getValue().summaryDate()).hasToString("2026-07-01");
        assertThat(commandCaptor.getValue().summaryJson()).contains("오늘 작업 요약");
    }

    @Test
    void preservesSuggestionOnlyWhenNoConfirmedDomainExistsYet() {
        TaskPublicService taskPublicService = mock(TaskPublicService.class);
        WbsItemPublicService wbsItemPublicService = mock(WbsItemPublicService.class);
        SchedulePublicService schedulePublicService = mock(SchedulePublicService.class);
        DailySummaryPublicService dailySummaryPublicService = mock(DailySummaryPublicService.class);
        AgentSuggestionDomainApplyService service = service(
                taskPublicService,
                wbsItemPublicService,
                schedulePublicService,
                dailySummaryPublicService
        );
        AgentSuggestion suggestion = suggestion(UUID.randomUUID(), AgentSuggestionType.DOCUMENT_DRAFT, Map.of(
                "title", "문서 초안",
                "description", "승인된 suggestion으로 보존"
        ));

        service.applyApprovedSuggestion(UUID.randomUUID(), suggestion);

        verifyNoInteractions(taskPublicService);
        verifyNoInteractions(wbsItemPublicService);
        verifyNoInteractions(schedulePublicService);
        verifyNoInteractions(dailySummaryPublicService);
    }

    private AgentSuggestionDomainApplyService service(
            TaskPublicService taskPublicService,
            WbsItemPublicService wbsItemPublicService,
            SchedulePublicService schedulePublicService,
            DailySummaryPublicService dailySummaryPublicService
    ) {
        return new AgentSuggestionDomainApplyService(
                taskPublicService,
                wbsItemPublicService,
                schedulePublicService,
                dailySummaryPublicService,
                new ObjectMapper()
        );
    }

    private AgentSuggestion suggestion(UUID roomId, AgentSuggestionType type, Map<String, Object> payload) {
        return AgentSuggestion.draft(
                UUID.randomUUID(),
                roomId,
                UUID.randomUUID(),
                null,
                type,
                payload,
                null
        );
    }
}
