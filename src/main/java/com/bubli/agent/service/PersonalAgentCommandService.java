package com.bubli.agent.service;

import com.bubli.agent.dto.PersonalAgentCommandResponse;
import com.bubli.agent.dto.PersonalAgentMemoryInput;
import com.bubli.agent.dto.PersonalAgentMemoryMessage;
import com.bubli.agent.dto.PersonalAgentMemorySummary;
import com.bubli.agent.dto.PersonalAgentMessageResponse;
import com.bubli.agent.dto.PersonalAgentSuggestionResponse;
import com.bubli.agent.model.AiCallExecutor;
import com.bubli.agent.type.AgentCommandMode;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.chat.type.MessageType;
import com.bubli.global.locale.SupportedLocale;
import com.bubli.personal.memo.dto.MemoResult;
import com.bubli.personal.memo.service.MemoPublicService;
import com.bubli.resource.dto.ResourceAnalysisSummaryResult;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.resource.type.ResourceVisibility;
import com.bubli.user.service.UserLocalePublicService;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.service.SchedulePublicService;
import com.bubli.work.task.dto.TaskResult;
import com.bubli.work.task.service.TaskPublicService;
import com.bubli.work.task.type.TaskStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalAgentCommandService {

	private static final String PROMPT_VERSION = "personal-agent-command-v1";
	private static final String SOURCE = "PERSONAL_AGENT_WIDGET";
	private static final int CONTEXT_LIMIT = 8;
	private static final int MEMORY_LIMIT = 12;
	private static final int SUMMARY_LIMIT = 5;
	private static final int RESOURCE_LIMIT = 5;

	private final TaskPublicService taskPublicService;
	private final SchedulePublicService schedulePublicService;
	private final MemoPublicService memoPublicService;
	private final ResourcePublicService resourcePublicService;
	private final UserLocalePublicService userLocalePublicService;
	private final ObjectProvider<ChatModel> chatModelProvider;
	private final ObjectProvider<AiCallExecutor> aiCallExecutorProvider;
	private final ObjectMapper objectMapper;

	@Transactional(readOnly = true)
	public PersonalAgentCommandResponse execute(
			UUID userId,
			String message,
			AgentCommandMode mode,
			List<UUID> resourceIds,
			PersonalAgentMemoryInput memory
	) {
		AgentCommandMode commandMode = mode == null ? AgentCommandMode.ANSWER : mode;
		PersonalAgentMemoryInput safeMemory = memory == null ? new PersonalAgentMemoryInput(List.of(), List.of()) : memory;
		String locale = SupportedLocale.normalize(userLocalePublicService.resolveLocaleCode(userId, null));
		PersonalContext context = collectContext(userId, resourceIds, safeMemory);
		List<PersonalAgentSuggestionResponse> suggestions = commandMode == AgentCommandMode.SUGGEST
				? List.of(createLocalSuggestion(message))
				: List.of();
		String answer = answer(message, commandMode, locale, context, suggestions);
		return new PersonalAgentCommandResponse(
				new PersonalAgentMessageResponse(
						"AGENT",
						MessageType.AGENT_RESPONSE,
						responseBody(message, commandMode, answer, context, suggestions),
						Instant.now()
				),
				suggestions
		);
	}

	private PersonalContext collectContext(UUID userId, List<UUID> resourceIds, PersonalAgentMemoryInput memory) {
		Instant now = Instant.now();
		Instant from = now.minus(30, ChronoUnit.DAYS);
		Instant to = now.plus(30, ChronoUnit.DAYS);
		List<TaskResult> tasks = taskPublicService.getPersonalContextTasks(userId, CONTEXT_LIMIT);
		List<ScheduleResult> schedules = schedulePublicService.getSchedulesBetween(userId, now.minus(7, ChronoUnit.DAYS), to)
				.stream()
				.filter(schedule -> schedule.roomId() == null)
				.limit(CONTEXT_LIMIT)
				.toList();
		List<MemoResult> memos = memoPublicService
				.getUpdatedMemosBetween(userId, from, now.plus(1, ChronoUnit.SECONDS), CONTEXT_LIMIT)
				.stream()
				.filter(memo -> memo.roomId() == null)
				.toList();
		List<ResourceAnalysisSummaryResult> analysisSummaries = resourcePublicService
				.getRecentAnalysisSummaries(userId, RESOURCE_LIMIT);
		List<ResourceResult> selectedResources = selectedPersonalResources(userId, resourceIds);
		return new PersonalContext(tasks, schedules, memos, analysisSummaries, selectedResources, memory);
	}

	private List<ResourceResult> selectedPersonalResources(UUID userId, List<UUID> resourceIds) {
		if (resourceIds == null || resourceIds.isEmpty()) {
			return List.of();
		}
		return resourceIds.stream()
				.distinct()
				.limit(RESOURCE_LIMIT)
				.map(resourceId -> resourcePublicService.getReadableResource(userId, resourceId))
				.filter(resource -> resource.visibility() == ResourceVisibility.PERSONAL)
				.filter(resource -> userId.equals(resource.ownerId()))
				.toList();
	}

	private String answer(
			String message,
			AgentCommandMode mode,
			String locale,
			PersonalContext context,
			List<PersonalAgentSuggestionResponse> suggestions
	) {
		ChatModel chatModel = chatModelProvider.getIfAvailable();
		if (chatModel == null) {
			return fallbackAnswer(mode, suggestions);
		}
		String prompt = prompt(message, mode, locale, context, suggestions);
		AiCallExecutor executor = aiCallExecutorProvider.getIfAvailable();
		try {
			if (executor == null) {
				return chatModel.call(prompt);
			}
			return executor.execute("personal-agent-command", () -> chatModel.call(prompt));
		} catch (RuntimeException exception) {
			log.warn("Personal agent command LLM answer failed.", exception);
			return fallbackAnswer(mode, suggestions);
		}
	}

	private String prompt(
			String message,
			AgentCommandMode mode,
			String locale,
			PersonalContext context,
			List<PersonalAgentSuggestionResponse> suggestions
	) {
		return """
				You are Bubli's personal widget agent.
				Write a concise response in this locale: %s.
				Mode: %s

				Use only the personal context and local chat memory below.
				Do not claim that local chat memory is stored on the server.
				If TODO or TASK suggestions are needed, describe them as local draft suggestions that the user can approve.

				User message:
				%s

				Personal context:
				%s

				Local draft suggestions already prepared by the backend response:
				%s
				""".formatted(locale, mode, message, context.promptBlock(), suggestionsBlock(suggestions));
	}

	private String fallbackAnswer(AgentCommandMode mode, List<PersonalAgentSuggestionResponse> suggestions) {
		if (mode == AgentCommandMode.SUGGEST && !suggestions.isEmpty()) {
			return "I prepared a local TODO draft. Review it in the personal agent widget before creating the task.";
		}
		return "I can help with your personal TODOs, schedules, memos, resources, and local chat memory.";
	}

	private PersonalAgentSuggestionResponse createLocalSuggestion(String message) {
		AgentSuggestionType type = inferSuggestionType(message);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("title", suggestionTitle(message));
		payload.put("description", message == null ? "" : message.trim());
		payload.put("status", TaskStatus.TODO.name());
		payload.put("dueAt", null);
		Map<String, Object> evidence = new LinkedHashMap<>();
		evidence.put("source", SOURCE);
		evidence.put("promptVersion", PROMPT_VERSION);
		evidence.put("serverPersisted", false);
		return new PersonalAgentSuggestionResponse(null, type, payload, evidence);
	}

	private AgentSuggestionType inferSuggestionType(String message) {
		String normalized = normalize(message);
		if (containsAny(normalized, "task")) {
			return AgentSuggestionType.TASK;
		}
		return AgentSuggestionType.TODO;
	}

	private String suggestionTitle(String message) {
		String normalized = message == null ? "" : message.replaceAll("\\s+", " ").trim();
		if (normalized.isBlank()) {
			return "Personal TODO";
		}
		return normalized.length() <= 80 ? normalized : normalized.substring(0, 80);
	}

	private JsonNode responseBody(
			String request,
			AgentCommandMode mode,
			String answer,
			PersonalContext context,
			List<PersonalAgentSuggestionResponse> suggestions
	) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("text", answer);
		body.put("request", request);
		body.put("mode", mode.name());
		body.put("promptVersion", PROMPT_VERSION);
		body.put("serverPersisted", false);
		body.put("contextCharacters", context.promptBlock().length());
		body.put("localSuggestionCount", suggestions.size());
		body.put("source", SOURCE);
		return objectMapper.valueToTree(body);
	}

	private String suggestionsBlock(List<PersonalAgentSuggestionResponse> suggestions) {
		if (suggestions.isEmpty()) {
			return "- none";
		}
		StringBuilder builder = new StringBuilder();
		for (PersonalAgentSuggestionResponse suggestion : suggestions) {
			builder.append("- type=")
					.append(suggestion.suggestionType())
					.append(", title=")
					.append(suggestion.payload().get("title"))
					.append('\n');
		}
		return builder.toString();
	}

	private boolean containsAny(String value, String... candidates) {
		for (String candidate : candidates) {
			if (value.contains(candidate.toLowerCase(Locale.ROOT))) {
				return true;
			}
		}
		return false;
	}

	private String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}

	private record PersonalContext(
			List<TaskResult> tasks,
			List<ScheduleResult> schedules,
			List<MemoResult> memos,
			List<ResourceAnalysisSummaryResult> analysisSummaries,
			List<ResourceResult> selectedResources,
			PersonalAgentMemoryInput memory
	) {
		private String promptBlock() {
			List<String> sections = new ArrayList<>();
			appendSection(sections, "Local chat summaries", memory.summaries().stream()
					.limit(SUMMARY_LIMIT)
					.map(this::summaryLine)
					.toList());
			appendSection(sections, "Local recent messages", memory.recentMessages().stream()
					.limit(MEMORY_LIMIT)
					.map(this::messageLine)
					.toList());
			appendSection(sections, "Personal TODOs", tasks.stream().map(this::taskLine).toList());
			appendSection(sections, "Personal schedules", schedules.stream().map(this::scheduleLine).toList());
			appendSection(sections, "Personal memos", memos.stream().map(this::memoLine).toList());
			appendSection(sections, "Personal resource summaries",
					analysisSummaries.stream().map(this::analysisSummaryLine).toList());
			appendSection(sections, "Selected personal resources",
					selectedResources.stream().map(this::resourceLine).toList());
			return sections.isEmpty() ? "No personal context was provided." : String.join("\n\n", sections);
		}

		private void appendSection(List<String> sections, String title, List<String> lines) {
			if (!lines.isEmpty()) {
				sections.add("[" + title + "]\n" + String.join("\n", lines));
			}
		}

		private String summaryLine(PersonalAgentMemorySummary summary) {
			return "- %s (%s..%s)".formatted(summary.summary(), summary.fromCreatedAt(), summary.toCreatedAt());
		}

		private String messageLine(PersonalAgentMemoryMessage message) {
			return "- %s: %s (%s)".formatted(message.role(), message.text(), message.createdAt());
		}

		private String taskLine(TaskResult task) {
			return "- taskId=%s title=%s status=%s dueAt=%s".formatted(
					task.id(),
					task.title(),
					task.status(),
					task.dueAt()
			);
		}

		private String scheduleLine(ScheduleResult schedule) {
			return "- scheduleId=%s title=%s startsAt=%s endsAt=%s".formatted(
					schedule.id(),
					schedule.title(),
					schedule.startsAt(),
					schedule.endsAt()
			);
		}

		private String memoLine(MemoResult memo) {
			return "- memoId=%s body=%s updatedAt=%s".formatted(memo.id(), memo.body(), memo.updatedAt());
		}

		private String analysisSummaryLine(ResourceAnalysisSummaryResult summary) {
			return "- resourceId=%s title=%s summary=%s updatedAt=%s".formatted(
					summary.resourceId(),
					summary.title(),
					summary.summary(),
					summary.updatedAt()
			);
		}

		private String resourceLine(ResourceResult resource) {
			return "- resourceId=%s title=%s kind=%s status=%s".formatted(
					resource.id(),
					resource.title(),
					resource.kind(),
					resource.status()
			);
		}
	}
}
