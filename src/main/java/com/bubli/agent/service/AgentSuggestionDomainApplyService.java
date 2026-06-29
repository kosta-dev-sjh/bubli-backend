package com.bubli.agent.service;

import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentSuggestionDomainApplyService {

    private final TaskPublicService taskPublicService;
    private final WbsItemPublicService wbsItemPublicService;
    private final SchedulePublicService schedulePublicService;
    private final DailySummaryPublicService dailySummaryPublicService;
    private final ObjectMapper objectMapper;

    public void applyApprovedSuggestion(UUID reviewerId, AgentSuggestion suggestion) {
        AgentSuggestionType type = suggestion.getSuggestionType();
        if (type == AgentSuggestionType.DAILY_SUMMARY) {
            upsertDailySummary(reviewerId, suggestion);
            return;
        }
        if (suggestion.getRoomId() == null) {
            return;
        }
        if (type == AgentSuggestionType.TASK || type == AgentSuggestionType.TODO) {
            createTask(reviewerId, suggestion);
            return;
        }
        if (type == AgentSuggestionType.WBS) {
            createWbsItem(reviewerId, suggestion);
            return;
        }
        if (type == AgentSuggestionType.SCHEDULE) {
            createSchedule(reviewerId, suggestion);
            return;
        }
        preserveApprovedSuggestion(suggestion);
    }

    private void createTask(UUID reviewerId, AgentSuggestion suggestion) {
        Map<String, Object> payload = suggestion.getPayloadJson();
        taskPublicService.createRoomTask(reviewerId, suggestion.getRoomId(), new CreateRoomTaskCommand(
                uuid(payload.get("assigneeUserId")),
                uuid(payload.get("wbsItemId")),
                requiredText(payload, "title"),
                text(payload.get("description")),
                enumValue(TaskStatus.class, payload.get("status"), TaskStatus.TODO),
                instant(payload.get("dueAt"))
        ));
    }

    private void createWbsItem(UUID reviewerId, AgentSuggestion suggestion) {
        Map<String, Object> payload = suggestion.getPayloadJson();
        wbsItemPublicService.create(reviewerId, suggestion.getRoomId(), new CreateWbsItemCommand(
                uuid(payload.get("parentId")),
                requiredText(payload, "title"),
                integer(payload.get("orderNo")),
                enumValue(WbsStatus.class, payload.get("status"), WbsStatus.TODO)
        ));
    }

    private void createSchedule(UUID reviewerId, AgentSuggestion suggestion) {
        Map<String, Object> payload = suggestion.getPayloadJson();
        schedulePublicService.create(reviewerId, new CreateScheduleCommand(
                suggestion.getRoomId(),
                uuid(payload.get("taskId")),
                uuid(payload.get("wbsItemId")),
                requiredText(payload, "title"),
                requiredInstant(payload, "startsAt"),
                instant(payload.get("endsAt")),
                bool(payload.get("allDay"))
        ));
    }

    private void upsertDailySummary(UUID reviewerId, AgentSuggestion suggestion) {
        Map<String, Object> payload = suggestion.getPayloadJson();
        dailySummaryPublicService.upsertDraft(reviewerId, new CreateDailySummaryDraftCommand(
                localDate(payload.get("summaryDate"), LocalDate.now()),
                summaryJson(payload)
        ));
    }

    private void preserveApprovedSuggestion(AgentSuggestion suggestion) {
        AgentSuggestionType type = suggestion.getSuggestionType();
        if (type == AgentSuggestionType.REQUIREMENT
                || type == AgentSuggestionType.QUESTION
                || type == AgentSuggestionType.REVIEW_ITEM
                || type == AgentSuggestionType.DOCUMENT_DRAFT
                || type == AgentSuggestionType.CONTRACT_FIELD
                || type == AgentSuggestionType.CONTRACT_REVIEW
                || type == AgentSuggestionType.MEMO) {
            return;
        }
        throw new BusinessException(ErrorCode.AGENT_400_001);
    }

    private String requiredText(Map<String, Object> payload, String field) {
        String value = text(payload.get(field));
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.AGENT_400_001);
        }
        return value;
    }

    private Instant requiredInstant(Map<String, Object> payload, String field) {
        Instant value = instant(payload.get(field));
        if (value == null) {
            throw new BusinessException(ErrorCode.AGENT_400_001);
        }
        return value;
    }

    private String text(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private UUID uuid(Object value) {
        String text = text(value);
        return text == null || text.isBlank() ? null : UUID.fromString(text);
    }

    private Instant instant(Object value) {
        String text = text(value);
        return text == null || text.isBlank() ? null : Instant.parse(text);
    }

    private LocalDate localDate(Object value, LocalDate defaultValue) {
        String text = text(value);
        return text == null || text.isBlank() ? defaultValue : LocalDate.parse(text);
    }

    private String summaryJson(Map<String, Object> payload) {
        String summaryJson = text(payload.get("summaryJson"));
        if (summaryJson != null && !summaryJson.isBlank()) {
            return summaryJson;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize daily summary suggestion payload.", exception);
        }
    }

    private Integer integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = text(value);
        return text == null || text.isBlank() ? null : Integer.parseInt(text);
    }

    private boolean bool(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = text(value);
        return text != null && Boolean.parseBoolean(text);
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, Object value, E defaultValue) {
        String text = text(value);
        return text == null || text.isBlank() ? defaultValue : Enum.valueOf(type, text);
    }
}
