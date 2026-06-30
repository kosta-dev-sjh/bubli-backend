package com.bubli.agent.service;

import com.bubli.agent.dispatch.AgentJobQueueMessage;
import com.bubli.agent.dto.AgentJobContext;
import com.bubli.agent.type.AgentJobType;
import com.bubli.chat.dto.ChatMessageContextResult;
import com.bubli.chat.service.ChatMessagePublicService;
import com.bubli.memory.dto.RoomMemorySummaryContextResult;
import com.bubli.memory.service.RoomMemoryPublicService;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.resource.dto.ResourceSummaryResult;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.service.SchedulePublicService;
import com.bubli.work.task.dto.TaskResult;
import com.bubli.work.task.service.TaskPublicService;
import com.bubli.work.wbs.dto.WbsItemResult;
import com.bubli.work.wbs.service.WbsItemPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentJobContextCollector {

	private static final int MAX_CONTEXT_CHARS = 6000;
	private static final ZoneId DEFAULT_SUMMARY_ZONE = ZoneId.of("Asia/Seoul");

	private final ProjectMembershipPublicService projectMembershipPublicService;
	private final ResourcePublicService resourcePublicService;
	private final TaskPublicService taskPublicService;
	private final WbsItemPublicService wbsItemPublicService;
	private final SchedulePublicService schedulePublicService;
	private final ChatMessagePublicService chatMessagePublicService;
	private final RoomMemoryPublicService roomMemoryPublicService;

	@Transactional(readOnly = true)
	public AgentJobContext collect(AgentJobQueueMessage message) {
		StringBuilder context = new StringBuilder();
		if (message.roomId() != null) {
			appendRoomContext(context, message);
		}
		if (message.jobType() == AgentJobType.DAILY_SUMMARY) {
			appendDailySummaryContext(context, message);
		}
		appendPersonalContext(context, message);
		if (context.isEmpty()) {
			return AgentJobContext.empty();
		}
		String promptBlock = truncate(context.toString().trim(), MAX_CONTEXT_CHARS);
		return new AgentJobContext(promptBlock, promptBlock.length());
	}

	private void appendRoomContext(StringBuilder context, AgentJobQueueMessage message) {
		projectMembershipPublicService.assertActiveMember(message.requestedByUserId(), message.roomId());
		appendSection(context, "Room resource summaries",
				resourcePublicService.getRecentRoomSummaries(message.requestedByUserId(), message.roomId(), 5).stream()
						.map(this::resourceSummaryLine)
						.toList());
		appendSection(context, "Room tasks",
				taskPublicService.getRecentRoomTasks(message.roomId(), 8).stream()
						.map(this::taskLine)
						.toList());
		appendSection(context, "Room WBS",
				wbsItemPublicService.getRoomContextItems(message.roomId(), 8).stream()
						.map(this::wbsLine)
						.toList());
		Instant now = Instant.now();
		appendSection(context, "Room schedules",
				schedulePublicService.getRoomSchedulesBetween(message.roomId(), now.minus(1, ChronoUnit.DAYS), now.plus(14, ChronoUnit.DAYS)).stream()
						.limit(8)
						.map(this::scheduleLine)
						.toList());
		appendSection(context, "Recent room chat",
				chatMessagePublicService.getRecentRoomMessages(message.requestedByUserId(), message.roomId(), 8).stream()
						.map(this::chatLine)
						.toList());
		appendSection(context, "Room memory summaries",
				roomMemoryPublicService.getRecentRoomMemories(message.requestedByUserId(), message.roomId(), 3).stream()
						.map(this::memoryLine)
						.toList());
	}

	private void appendPersonalContext(StringBuilder context, AgentJobQueueMessage message) {
		appendSection(context, "Requester personal tasks",
				taskPublicService.getPersonalContextTasks(message.requestedByUserId(), 8).stream()
						.map(this::taskLine)
						.toList());
		Instant now = Instant.now();
		appendSection(context, "Requester schedules",
				schedulePublicService.getSchedulesBetween(message.requestedByUserId(), now.minus(1, ChronoUnit.DAYS), now.plus(14, ChronoUnit.DAYS)).stream()
						.limit(8)
						.map(this::scheduleLine)
						.toList());
	}

	private void appendDailySummaryContext(StringBuilder context, AgentJobQueueMessage message) {
		ZoneId zoneId = summaryZone(message.requestPayload());
		LocalDate summaryDate = summaryDate(message.requestPayload(), zoneId);
		Instant from = summaryDate.atStartOfDay(zoneId).toInstant();
		Instant to = summaryDate.plusDays(1).atStartOfDay(zoneId).toInstant();

		appendSection(context, "Daily summary target", List.of(
				"summaryDate=%s timezone=%s from=%s to=%s".formatted(summaryDate, zoneId, from, to)
		));
		appendSection(context, "Daily summary due tasks",
				taskPublicService.getDueBetweenTasks(message.requestedByUserId(), from, to).stream()
						.limit(20)
						.map(this::taskLine)
						.toList());
		appendSection(context, "Daily summary schedules",
				schedulePublicService.getSchedulesBetween(message.requestedByUserId(), from, to).stream()
						.limit(20)
						.map(this::scheduleLine)
						.toList());
	}

	private LocalDate summaryDate(Map<String, Object> payload, ZoneId zoneId) {
		Object value = payload == null ? null : payload.get("summaryDate");
		if (value == null || value.toString().isBlank()) {
			return LocalDate.now(zoneId);
		}
		try {
			return LocalDate.parse(value.toString());
		} catch (DateTimeParseException exception) {
			return LocalDate.now(zoneId);
		}
	}

	private ZoneId summaryZone(Map<String, Object> payload) {
		Object value = payload == null ? null : payload.get("timezone");
		if (value == null || value.toString().isBlank()) {
			return DEFAULT_SUMMARY_ZONE;
		}
		try {
			return ZoneId.of(value.toString());
		} catch (DateTimeException exception) {
			return DEFAULT_SUMMARY_ZONE;
		}
	}

	private void appendSection(StringBuilder context, String title, List<String> lines) {
		if (lines.isEmpty()) {
			return;
		}
		context.append("\n[").append(title).append("]\n");
		for (String line : lines) {
			context.append("- ").append(truncate(line, 500)).append('\n');
		}
	}

	private String resourceSummaryLine(ResourceSummaryResult summary) {
		return "resourceId=%s summary=%s".formatted(summary.resourceId(), summary.summaryJson());
	}

	private String taskLine(TaskResult task) {
		return "taskId=%s status=%s dueAt=%s title=%s description=%s".formatted(
				task.id(),
				task.status(),
				task.dueAt(),
				task.title(),
				task.description()
		);
	}

	private String wbsLine(WbsItemResult item) {
		return "wbsId=%s parentId=%s status=%s order=%s title=%s".formatted(
				item.id(),
				item.parentId(),
				item.status(),
				item.orderNo(),
				item.title()
		);
	}

	private String scheduleLine(ScheduleResult schedule) {
		return "scheduleId=%s roomId=%s startsAt=%s endsAt=%s title=%s".formatted(
				schedule.id(),
				schedule.roomId(),
				schedule.startsAt(),
				schedule.endsAt(),
				schedule.title()
		);
	}

	private String chatLine(ChatMessageContextResult message) {
		return "seq=%s type=%s resourceId=%s body=%s".formatted(
				message.roomSequence(),
				message.messageType(),
				message.resourceId(),
				message.body()
		);
	}

	private String memoryLine(RoomMemorySummaryContextResult memory) {
		return "seq=%s-%s status=%s summary=%s".formatted(
				memory.fromSequence(),
				memory.toSequence(),
				memory.status(),
				memory.summaryJson()
		);
	}

	private String truncate(String text, int limit) {
		if (text == null || text.length() <= limit) {
			return text;
		}
		return text.substring(0, limit);
	}
}
