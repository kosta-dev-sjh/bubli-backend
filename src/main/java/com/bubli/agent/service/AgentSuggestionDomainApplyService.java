package com.bubli.agent.service;

import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.work.schedule.dto.CreateScheduleCommand;
import com.bubli.work.schedule.service.SchedulePublicService;
import com.bubli.work.task.dto.CreateRoomTaskCommand;
import com.bubli.work.task.service.TaskPublicService;
import com.bubli.work.task.type.TaskStatus;
import com.bubli.work.wbs.dto.CreateWbsItemCommand;
import com.bubli.work.wbs.service.WbsItemPublicService;
import com.bubli.work.wbs.type.WbsStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentSuggestionDomainApplyService {

    private final TaskPublicService taskPublicService;
    private final WbsItemPublicService wbsItemPublicService;
    private final SchedulePublicService schedulePublicService;

    public void applyApprovedSuggestion(UUID reviewerId, AgentSuggestion suggestion) {
        if (suggestion.getRoomId() == null) {
            return;
        }
        AgentSuggestionType type = suggestion.getSuggestionType();
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
        }
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
