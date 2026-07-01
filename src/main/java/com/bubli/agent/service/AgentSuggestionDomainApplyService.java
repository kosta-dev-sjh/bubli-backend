package com.bubli.agent.service;

import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.memory.dto.CreateDailySummaryDraftCommand;
import com.bubli.memory.service.DailySummaryPublicService;
import com.bubli.personal.memo.dto.CreateMemoCommand;
import com.bubli.personal.memo.service.MemoPublicService;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentSuggestionDomainApplyService {

    private final TaskPublicService taskPublicService;
    private final WbsItemPublicService wbsItemPublicService;
    private final SchedulePublicService schedulePublicService;
    private final DailySummaryPublicService dailySummaryPublicService;
    private final GeneratedDocumentService generatedDocumentService;
    private final MemoPublicService memoPublicService;
    private final ObjectMapper objectMapper;

    public void applyApprovedSuggestion(UUID reviewerId, AgentSuggestion suggestion) {
        AgentSuggestionType type = suggestion.getSuggestionType();
        if (type == AgentSuggestionType.DAILY_SUMMARY) {
            markApplied(suggestion, "DAILY_SUMMARY", upsertDailySummary(reviewerId, suggestion));
            return;
        }
        if (type == AgentSuggestionType.DOCUMENT_DRAFT) {
            markApplied(suggestion, "GENERATED_DOCUMENT", createGeneratedDocument(reviewerId, suggestion));
            return;
        }
        if (type == AgentSuggestionType.MEMO) {
            markApplied(suggestion, "MEMO", createMemo(reviewerId, suggestion));
            return;
        }
        if (suggestion.getRoomId() == null) {
            markApplied(suggestion, "CONFIRMED_SUGGESTION", preservedDetails(
                    type,
                    "No roomId for domain materialization."
            ));
            return;
        }
        switch (type) {
            case TASK, TODO -> markApplied(suggestion, "TASK", createTask(reviewerId, suggestion));
            case WBS -> markApplied(suggestion, "WBS", createWbsItem(reviewerId, suggestion));
            case SCHEDULE -> markApplied(suggestion, "SCHEDULE", createSchedule(reviewerId, suggestion));
            case REQUIREMENT -> markApplied(suggestion, "CONFIRMED_REQUIREMENT", preservedDetails(
                    type,
                    "Approved suggestion is the confirmed requirement because a separate requirement table is not defined."
            ));
            case QUESTION -> markApplied(suggestion, "CONFIRMATION_QUESTION", preservedDetails(
                    type,
                    "Approved suggestion remains available through confirmation item queries."
            ));
            case REVIEW_ITEM -> markApplied(suggestion, "CONFIRMATION_REVIEW_ITEM", preservedDetails(
                    type,
                    "Approved suggestion remains available through confirmation item queries."
            ));
            case CONTRACT_FIELD -> markApplied(suggestion, "CONTRACT_FIELD_REFERENCE", contractReferenceDetails(suggestion));
            case CONTRACT_REVIEW -> markApplied(suggestion, "CONTRACT_REVIEW_NOTE", contractReviewDetails(type));
            default -> throw new BusinessException(ErrorCode.AGENT_400_001);
        }
    }

    private Map<String, Object> createTask(UUID reviewerId, AgentSuggestion suggestion) {
        Map<String, Object> payload = suggestion.getPayloadJson();
        var result = taskPublicService.createRoomTask(reviewerId, suggestion.getRoomId(), new CreateRoomTaskCommand(
                uuid(payload.get("assigneeUserId")),
                uuid(payload.get("wbsItemId")),
                requiredText(payload, "title"),
                text(payload.get("description")),
                enumValue(TaskStatus.class, payload.get("status"), TaskStatus.TODO),
                instant(payload.get("dueAt"))
        ));
        return result == null ? Map.of() : Map.of("taskId", result.id().toString());
    }

    private Map<String, Object> createWbsItem(UUID reviewerId, AgentSuggestion suggestion) {
        Map<String, Object> payload = suggestion.getPayloadJson();
        var result = wbsItemPublicService.create(reviewerId, suggestion.getRoomId(), new CreateWbsItemCommand(
                uuid(payload.get("parentId")),
                requiredText(payload, "title"),
                integer(payload.get("orderNo")),
                enumValue(WbsStatus.class, payload.get("status"), WbsStatus.TODO)
        ));
        return result == null ? Map.of() : Map.of("wbsItemId", result.id().toString());
    }

    private Map<String, Object> createSchedule(UUID reviewerId, AgentSuggestion suggestion) {
        Map<String, Object> payload = suggestion.getPayloadJson();
        var result = schedulePublicService.create(reviewerId, new CreateScheduleCommand(
                suggestion.getRoomId(),
                uuid(payload.get("taskId")),
                uuid(payload.get("wbsItemId")),
                requiredText(payload, "title"),
                requiredInstant(payload, "startsAt"),
                instant(payload.get("endsAt")),
                bool(payload.get("allDay"))
        ));
        return result == null ? Map.of() : Map.of("scheduleId", result.id().toString());
    }

    private Map<String, Object> upsertDailySummary(UUID reviewerId, AgentSuggestion suggestion) {
        Map<String, Object> payload = suggestion.getPayloadJson();
        var result = dailySummaryPublicService.upsertDraft(reviewerId, new CreateDailySummaryDraftCommand(
                localDate(payload.get("summaryDate"), LocalDate.now()),
                summaryJson(payload)
        ));
        return result == null ? Map.of() : Map.of("dailySummaryId", result.id().toString());
    }

    private Map<String, Object> createGeneratedDocument(UUID reviewerId, AgentSuggestion suggestion) {
        var result = generatedDocumentService.createFromSuggestion(reviewerId, suggestion);
        return Map.of("generatedDocumentId", result.getId().toString());
    }

    private Map<String, Object> createMemo(UUID reviewerId, AgentSuggestion suggestion) {
        Map<String, Object> payload = suggestion.getPayloadJson();
        String body = text(payload.get("body"));
        if (body == null || body.isBlank()) {
            body = requiredText(payload, "description");
        }
        var result = suggestion.getRoomId() == null
                ? memoPublicService.createPersonalMemo(reviewerId, new CreateMemoCommand(body))
                : memoPublicService.createRoomMemo(reviewerId, suggestion.getRoomId(), new CreateMemoCommand(body));
        return result == null ? Map.of() : Map.of("memoId", result.id().toString());
    }

    private Map<String, Object> preservedDetails(AgentSuggestionType type, String reason) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("suggestionType", type.name());
        details.put("policy", "APPROVED_SUGGESTION_IS_CONFIRMED_RESULT");
        details.put("materialized", false);
        details.put("reason", reason);
        return details;
    }

    private Map<String, Object> contractReferenceDetails(AgentSuggestion suggestion) {
        Map<String, Object> payload = suggestion.getPayloadJson();
        Map<String, Object> details = preservedDetails(
                AgentSuggestionType.CONTRACT_FIELD,
                "Contract field suggestions are preserved as project-room reference values and are not auto-applied."
        );
        putIfPresent(details, "fieldKey", payload.get("fieldKey"));
        putIfPresent(details, "value", payload.get("value"));
        details.put("legalDisclaimer", "This is a reference extraction, not legal advice or legal judgment.");
        return details;
    }

    private Map<String, Object> contractReviewDetails(AgentSuggestionType type) {
        Map<String, Object> details = preservedDetails(
                type,
                "Contract review suggestions are preserved as review notes and are not auto-applied."
        );
        details.put("legalDisclaimer", "This is a reference extraction, not legal advice or legal judgment.");
        return details;
    }

    private void putIfPresent(Map<String, Object> details, String key, Object value) {
        if (value != null) {
            details.put(key, value);
        }
    }

    private void markApplied(AgentSuggestion suggestion, String targetType, Map<String, Object> details) {
        Map<String, Object> payload = new LinkedHashMap<>(suggestion.getPayloadJson());
        payload.put("appliedResult", Map.of(
                "targetType", targetType,
                "details", details,
                "appliedAt", Instant.now().toString()
        ));
        suggestion.update(null, payload, null);
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
