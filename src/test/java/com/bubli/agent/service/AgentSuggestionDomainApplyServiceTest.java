package com.bubli.agent.service;

import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.entity.GeneratedDocument;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.memory.dto.CreateDailySummaryDraftCommand;
import com.bubli.memory.service.DailySummaryPublicService;
import com.bubli.personal.memo.dto.CreateMemoCommand;
import com.bubli.personal.memo.dto.MemoResult;
import com.bubli.personal.memo.service.MemoPublicService;
import com.bubli.personal.memo.type.MemoStatus;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
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
        AgentSuggestionDomainApplyService service = service(taskPublicService, mock(WbsItemPublicService.class), mock(SchedulePublicService.class), mock(DailySummaryPublicService.class), mock(GeneratedDocumentService.class), mock(MemoPublicService.class));
        UUID reviewerId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID wbsItemId = UUID.randomUUID();
        AgentSuggestion suggestion = suggestion(roomId, AgentSuggestionType.TASK, Map.of(
                "title", "API 援ы쁽",
                "description", "濡쒓렇??API 援ы쁽",
                "assigneeUserId", assigneeId.toString(),
                "wbsItemId", wbsItemId.toString(),
                "status", "TODO",
                "dueAt", "2026-07-01T00:00:00Z"
        ));

        service.applyApprovedSuggestion(reviewerId, suggestion);

        ArgumentCaptor<CreateRoomTaskCommand> commandCaptor = ArgumentCaptor.forClass(CreateRoomTaskCommand.class);
        verify(taskPublicService).createRoomTask(org.mockito.ArgumentMatchers.eq(reviewerId), org.mockito.ArgumentMatchers.eq(roomId), commandCaptor.capture());
        assertThat(commandCaptor.getValue().title()).isEqualTo("API 援ы쁽");
        assertThat(commandCaptor.getValue().status()).isEqualTo(TaskStatus.TODO);
        assertThat(commandCaptor.getValue().assigneeUserId()).isEqualTo(assigneeId);
        assertThat(commandCaptor.getValue().wbsItemId()).isEqualTo(wbsItemId);
    }

    @Test
    void appliesWbsSuggestionToWbsItem() {
        WbsItemPublicService wbsItemPublicService = mock(WbsItemPublicService.class);
        AgentSuggestionDomainApplyService service = service(mock(TaskPublicService.class), wbsItemPublicService, mock(SchedulePublicService.class), mock(DailySummaryPublicService.class), mock(GeneratedDocumentService.class), mock(MemoPublicService.class));
        UUID reviewerId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        AgentSuggestion suggestion = suggestion(roomId, AgentSuggestionType.WBS, Map.of(
                "title", "?붽뎄?ы빆 ?뺣━",
                "parentId", parentId.toString(),
                "orderNo", 3,
                "status", "IN_PROGRESS"
        ));

        service.applyApprovedSuggestion(reviewerId, suggestion);

        ArgumentCaptor<CreateWbsItemCommand> commandCaptor = ArgumentCaptor.forClass(CreateWbsItemCommand.class);
        verify(wbsItemPublicService).create(org.mockito.ArgumentMatchers.eq(reviewerId), org.mockito.ArgumentMatchers.eq(roomId), commandCaptor.capture());
        assertThat(commandCaptor.getValue().title()).isEqualTo("?붽뎄?ы빆 ?뺣━");
        assertThat(commandCaptor.getValue().parentId()).isEqualTo(parentId);
        assertThat(commandCaptor.getValue().orderNo()).isEqualTo(3);
        assertThat(commandCaptor.getValue().status()).isEqualTo(WbsStatus.IN_PROGRESS);
    }

    @Test
    void appliesScheduleSuggestionToSchedule() {
        SchedulePublicService schedulePublicService = mock(SchedulePublicService.class);
        AgentSuggestionDomainApplyService service = service(mock(TaskPublicService.class), mock(WbsItemPublicService.class), schedulePublicService, mock(DailySummaryPublicService.class), mock(GeneratedDocumentService.class), mock(MemoPublicService.class));
        UUID reviewerId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        Instant startsAt = Instant.parse("2026-07-01T01:00:00Z");
        Instant endsAt = Instant.parse("2026-07-01T02:00:00Z");
        AgentSuggestion suggestion = suggestion(roomId, AgentSuggestionType.SCHEDULE, Map.of(
                "title", "?뚯쓽",
                "startsAt", startsAt.toString(),
                "endsAt", endsAt.toString(),
                "allDay", false
        ));

        service.applyApprovedSuggestion(reviewerId, suggestion);

        ArgumentCaptor<CreateScheduleCommand> commandCaptor = ArgumentCaptor.forClass(CreateScheduleCommand.class);
        verify(schedulePublicService).create(org.mockito.ArgumentMatchers.eq(reviewerId), commandCaptor.capture());
        assertThat(commandCaptor.getValue().roomId()).isEqualTo(roomId);
        assertThat(commandCaptor.getValue().title()).isEqualTo("?뚯쓽");
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
                dailySummaryPublicService,
                mock(GeneratedDocumentService.class),
                mock(MemoPublicService.class)
        );
        UUID reviewerId = UUID.randomUUID();
        AgentSuggestion suggestion = suggestion(null, AgentSuggestionType.DAILY_SUMMARY, Map.of(
                "summaryDate", "2026-07-01",
                "summaryJson", "{\"summary\":\"?ㅻ뒛 ?묒뾽 ?붿빟\"}"
        ));

        service.applyApprovedSuggestion(reviewerId, suggestion);

        ArgumentCaptor<CreateDailySummaryDraftCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateDailySummaryDraftCommand.class);
        verify(dailySummaryPublicService).upsertDraft(org.mockito.ArgumentMatchers.eq(reviewerId), commandCaptor.capture());
        assertThat(commandCaptor.getValue().summaryDate()).hasToString("2026-07-01");
        assertThat(commandCaptor.getValue().summaryJson()).contains("?ㅻ뒛 ?묒뾽 ?붿빟");
    }

    @Test
    void appliesDocumentDraftSuggestionToGeneratedDocument() {
        TaskPublicService taskPublicService = mock(TaskPublicService.class);
        WbsItemPublicService wbsItemPublicService = mock(WbsItemPublicService.class);
        SchedulePublicService schedulePublicService = mock(SchedulePublicService.class);
        DailySummaryPublicService dailySummaryPublicService = mock(DailySummaryPublicService.class);
        GeneratedDocumentService generatedDocumentService = mock(GeneratedDocumentService.class);
        UUID generatedDocumentId = UUID.randomUUID();
        GeneratedDocument generatedDocument = GeneratedDocument.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "문서 초안",
                "MEETING_NOTE",
                "# Draft",
                Map.of()
        );
        ReflectionTestUtils.setField(generatedDocument, "id", generatedDocumentId);
        AgentSuggestionDomainApplyService service = service(
                taskPublicService,
                wbsItemPublicService,
                schedulePublicService,
                dailySummaryPublicService,
                generatedDocumentService,
                mock(MemoPublicService.class)
        );
        AgentSuggestion suggestion = suggestion(UUID.randomUUID(), AgentSuggestionType.DOCUMENT_DRAFT, Map.of(
                "title", "문서 초안",
                "documentType", "MEETING_NOTE",
                "contentMarkdown", "# Draft"
        ));
        org.mockito.Mockito.when(generatedDocumentService.createFromSuggestion(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(suggestion)
        )).thenReturn(generatedDocument);

        UUID reviewerId = UUID.randomUUID();
        service.applyApprovedSuggestion(reviewerId, suggestion);

        verify(generatedDocumentService).createFromSuggestion(reviewerId, suggestion);
        verifyNoInteractions(taskPublicService);
        verifyNoInteractions(wbsItemPublicService);
        verifyNoInteractions(schedulePublicService);
        verifyNoInteractions(dailySummaryPublicService);
        assertThat(suggestion.getPayloadJson())
                .extracting(payload -> ((Map<?, ?>) payload.get("appliedResult")).get("targetType"))
                .isEqualTo("GENERATED_DOCUMENT");
        assertThat(suggestion.getPayloadJson().toString()).contains(generatedDocumentId.toString());
    }

    @Test
    void appliesMemoSuggestionToMemo() {
        MemoPublicService memoPublicService = mock(MemoPublicService.class);
        AgentSuggestionDomainApplyService service = service(
                mock(TaskPublicService.class),
                mock(WbsItemPublicService.class),
                mock(SchedulePublicService.class),
                mock(DailySummaryPublicService.class),
                mock(GeneratedDocumentService.class),
                memoPublicService
        );
        UUID reviewerId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID memoId = UUID.randomUUID();
        org.mockito.Mockito.when(memoPublicService.createRoomMemo(
                org.mockito.ArgumentMatchers.eq(reviewerId),
                org.mockito.ArgumentMatchers.eq(roomId),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(new MemoResult(
                memoId,
                reviewerId,
                roomId,
                "회의 후 확인할 내용",
                MemoStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        ));
        AgentSuggestion suggestion = suggestion(roomId, AgentSuggestionType.MEMO, Map.of(
                "body", "회의 후 확인할 내용"
        ));

        service.applyApprovedSuggestion(reviewerId, suggestion);

        ArgumentCaptor<CreateMemoCommand> commandCaptor = ArgumentCaptor.forClass(CreateMemoCommand.class);
        verify(memoPublicService).createRoomMemo(
                org.mockito.ArgumentMatchers.eq(reviewerId),
                org.mockito.ArgumentMatchers.eq(roomId),
                commandCaptor.capture()
        );
        assertThat(commandCaptor.getValue().body()).isEqualTo("회의 후 확인할 내용");
        assertThat(appliedResult(suggestion).get("targetType")).isEqualTo("MEMO");
        assertThat(suggestion.getPayloadJson().toString()).contains(memoId.toString());
    }

    @Test
    void appliesPreservedSuggestionTypesWithExplicitTargetTypes() {
        AgentSuggestionDomainApplyService service = service(
                mock(TaskPublicService.class),
                mock(WbsItemPublicService.class),
                mock(SchedulePublicService.class),
                mock(DailySummaryPublicService.class),
                mock(GeneratedDocumentService.class),
                mock(MemoPublicService.class)
        );
        UUID reviewerId = UUID.randomUUID();

        List.of(
                Map.entry(AgentSuggestionType.REQUIREMENT, "CONFIRMED_REQUIREMENT"),
                Map.entry(AgentSuggestionType.QUESTION, "CONFIRMATION_QUESTION"),
                Map.entry(AgentSuggestionType.REVIEW_ITEM, "CONFIRMATION_REVIEW_ITEM"),
                Map.entry(AgentSuggestionType.CONTRACT_FIELD, "CONTRACT_FIELD_REFERENCE"),
                Map.entry(AgentSuggestionType.CONTRACT_REVIEW, "CONTRACT_REVIEW_NOTE")
        ).forEach(entry -> {
            AgentSuggestion suggestion = suggestion(UUID.randomUUID(), entry.getKey(), Map.of(
                    "title", entry.getKey().name(),
                    "fieldKey", "contractAmount",
                    "value", "3000000"
            ));

            service.applyApprovedSuggestion(reviewerId, suggestion);

            assertThat(appliedResult(suggestion).get("targetType")).isEqualTo(entry.getValue());
            assertThat(((Map<?, ?>) appliedResult(suggestion).get("details")).get("policy"))
                    .isEqualTo("APPROVED_SUGGESTION_IS_CONFIRMED_RESULT");
        });
    }

    private AgentSuggestionDomainApplyService service(
            TaskPublicService taskPublicService,
            WbsItemPublicService wbsItemPublicService,
            SchedulePublicService schedulePublicService,
            DailySummaryPublicService dailySummaryPublicService,
            GeneratedDocumentService generatedDocumentService,
            MemoPublicService memoPublicService
    ) {
        return new AgentSuggestionDomainApplyService(
                taskPublicService,
                wbsItemPublicService,
                schedulePublicService,
                dailySummaryPublicService,
                generatedDocumentService,
                memoPublicService,
                new ObjectMapper()
        );
    }

    private AgentSuggestion suggestion(UUID roomId, AgentSuggestionType type, Map<String, Object> payload) {
        AgentSuggestion suggestion = AgentSuggestion.draft(
                UUID.randomUUID(),
                roomId,
                UUID.randomUUID(),
                null,
                type,
                payload,
                null
        );
        ReflectionTestUtils.setField(suggestion, "id", UUID.randomUUID());
        return suggestion;
    }

    private Map<?, ?> appliedResult(AgentSuggestion suggestion) {
        return (Map<?, ?>) suggestion.getPayloadJson().get("appliedResult");
    }
}
