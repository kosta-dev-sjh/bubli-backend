package com.bubli.agent.service;

import com.bubli.agent.dispatch.AgentJobQueueMessage;
import com.bubli.agent.dto.AgentJobContext;
import com.bubli.agent.dto.AgentSuggestionResponse;
import com.bubli.agent.dto.ProjectRoomAgentCommandResponse;
import com.bubli.agent.model.AiCallExecutor;
import com.bubli.agent.type.AgentCommandMode;
import com.bubli.agent.type.AgentJobType;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.chat.dto.ChatMessageResponse;
import com.bubli.chat.service.ChatMessagePublicService;
import com.bubli.memory.dto.RoomMemorySummaryContextResult;
import com.bubli.memory.service.RoomMemoryPublicService;
import com.bubli.project.service.ProjectRoomEventPublicService;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectRoomAgentCommandService {

	private static final String PROMPT_VERSION = "project-room-agent-command-v1";

	private final ProjectMembershipPublicService projectMembershipPublicService;
	private final AgentJobContextCollector contextCollector;
	private final ChatMessagePublicService chatMessagePublicService;
	private final RoomMemoryPublicService roomMemoryPublicService;
	private final AgentSuggestionCommandService agentSuggestionCommandService;
	private final ProjectRoomEventPublicService projectRoomEventPublicService;
	private final ObjectProvider<ChatModel> chatModelProvider;
	private final ObjectProvider<AiCallExecutor> aiCallExecutorProvider;
	private final ObjectMapper objectMapper;

	@Transactional
	public ProjectRoomAgentCommandResponse execute(
			UUID userId,
			UUID roomId,
			String message,
			AgentCommandMode mode,
			List<UUID> resourceIds
	) {
		projectMembershipPublicService.assertActiveMember(userId, roomId);
		AgentCommandMode commandMode = mode == null ? AgentCommandMode.ANSWER : mode;
		AgentJobContext context = contextCollector.collect(new AgentJobQueueMessage(
				UUID.randomUUID(),
				userId,
				roomId,
				resourceIds == null || resourceIds.isEmpty() ? null : resourceIds.getFirst(),
				AgentJobType.GENERATE_QUESTIONS,
				Map.of(
						"source", "PROJECT_ROOM_AGENT_COMMAND",
						"mode", commandMode.name(),
						"message", message,
						"resourceIds", resourceIds == null ? List.of() : resourceIds
				),
				java.time.Instant.now()
		));
		String answer = answer(message, commandMode, context);
		List<AgentSuggestionResponse> suggestions = createSuggestions(
				userId,
				roomId,
				message,
				commandMode,
				answer,
				context,
				resourceIds
		);
		ChatMessageResponse chatMessage = ChatMessageResponse.from(chatMessagePublicService.createRoomAgentResponse(
				userId,
				roomId,
				responseBody(message, commandMode, answer, context, suggestions),
				resourceIds == null || resourceIds.isEmpty() ? null : resourceIds.getFirst()
		));
		RoomMemorySummaryContextResult memory = roomMemoryPublicService.createDraft(
				userId,
				roomId,
				chatMessage.roomSequence(),
				chatMessage.roomSequence(),
				memoryJson(message, commandMode, answer, context, suggestions)
		);
		return new ProjectRoomAgentCommandResponse(chatMessage, memory, suggestions);
	}

	private String answer(String message, AgentCommandMode mode, AgentJobContext context) {
		ChatModel chatModel = chatModelProvider.getIfAvailable();
		if (chatModel == null) {
			return fallbackAnswer(message, mode, context);
		}
		AiCallExecutor executor = aiCallExecutorProvider.getIfAvailable();
		String prompt = prompt(message, mode, context);
		if (executor == null) {
			return chatModel.call(prompt);
		}
		return executor.execute("project-room-agent-command", () -> chatModel.call(prompt));
	}

	private String prompt(String message, AgentCommandMode mode, AgentJobContext context) {
		return """
				You are Bubli's project room agent. Write a concise Korean response.
				Mode: %s
				Do not invent confirmed facts. Use the provided project context and say what should be checked when context is insufficient.

				User message:
				%s

				Project context:
				%s
				""".formatted(mode, message, context.promptBlock());
	}

	private String fallbackAnswer(String message, AgentCommandMode mode, AgentJobContext context) {
		return switch (mode) {
			case ANSWER -> "현재 수집된 프로젝트 맥락을 기준으로 확인했습니다. 요청: %s".formatted(message);
			case SUMMARIZE -> "현재 프로젝트 맥락 요약입니다. %s".formatted(context.promptBlock());
			case SUGGEST -> "다음 조치를 제안합니다: 요청 내용을 기준으로 TODO 또는 검토 항목을 생성해 승인 흐름에서 확정하세요.";
		};
	}

	private List<AgentSuggestionResponse> createSuggestions(
			UUID userId,
			UUID roomId,
			String message,
			AgentCommandMode mode,
			String answer,
			AgentJobContext context,
			List<UUID> resourceIds
	) {
		if (mode != AgentCommandMode.SUGGEST) {
			return List.of();
		}
		AgentSuggestionType suggestionType = inferSuggestionType(message);
		AgentSuggestionResponse suggestion = agentSuggestionCommandService.createDraft(
				userId,
				roomId,
				null,
				resourceIds == null || resourceIds.isEmpty() ? null : resourceIds.getFirst(),
				suggestionType,
				suggestionPayload(suggestionType, message, answer),
				suggestionEvidence(context, resourceIds)
		);
		projectRoomEventPublicService.recordAgentSuggestionsCreated(
				userId,
				roomId,
				List.of(suggestion.suggestionId()),
				List.of(suggestion.suggestionType().name())
		);
		return List.of(suggestion);
	}

	private AgentSuggestionType inferSuggestionType(String message) {
		String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);
		if (containsAny(normalized, "?", "질문", "확인", "물어", "문의", "애매", "누락", "불명확",
				"question", "ask", "unclear", "missing")) {
			return AgentSuggestionType.QUESTION;
		}
		if (containsAny(normalized, "검토", "리뷰", "위험", "리스크", "이슈", "조건", "계약", "확인 필요",
				"review", "risk", "issue", "condition", "contract")) {
			return AgentSuggestionType.REVIEW_ITEM;
		}
		return AgentSuggestionType.TODO;
	}

	private boolean containsAny(String value, String... candidates) {
		for (String candidate : candidates) {
			if (value.contains(candidate)) {
				return true;
			}
		}
		return false;
	}

	private Map<String, Object> suggestionPayload(AgentSuggestionType suggestionType, String message, String answer) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", suggestionType.name());
		payload.put("title", suggestionTitle(message, suggestionType));
		payload.put("description", message);
		payload.put("agentResponse", answer);
		payload.put("source", "PROJECT_ROOM_AGENT_COMMAND");
		return payload;
	}

	private String suggestionTitle(String message, AgentSuggestionType suggestionType) {
		String normalized = message == null ? "" : message.replaceAll("\\s+", " ").trim();
		if (normalized.isBlank()) {
			return switch (suggestionType) {
				case QUESTION -> "확인 질문";
				case REVIEW_ITEM -> "검토 항목";
				default -> "TODO 후보";
			};
		}
		return normalized.length() <= 80 ? normalized : normalized.substring(0, 80);
	}

	private Map<String, Object> suggestionEvidence(AgentJobContext context, List<UUID> resourceIds) {
		Map<String, Object> evidence = new LinkedHashMap<>();
		evidence.put("source", "PROJECT_ROOM_AGENT_COMMAND");
		evidence.put("promptVersion", PROMPT_VERSION);
		evidence.put("contextCharacters", context.characterCount());
		evidence.put("resourceIds", resourceIds == null ? List.of() : resourceIds);
		return evidence;
	}

	private JsonNode responseBody(
			String request,
			AgentCommandMode mode,
			String answer,
			AgentJobContext context,
			List<AgentSuggestionResponse> suggestions
	) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("text", answer);
		body.put("request", request);
		body.put("mode", mode.name());
		body.put("promptVersion", PROMPT_VERSION);
		body.put("contextCharacters", context.characterCount());
		body.put("suggestionIds", suggestions.stream()
				.map(AgentSuggestionResponse::suggestionId)
				.toList());
		return objectMapper.valueToTree(body);
	}

	private String memoryJson(
			String request,
			AgentCommandMode mode,
			String answer,
			AgentJobContext context,
			List<AgentSuggestionResponse> suggestions
	) {
		Map<String, Object> memory = new LinkedHashMap<>();
		memory.put("source", "PROJECT_ROOM_AGENT_COMMAND");
		memory.put("mode", mode.name());
		memory.put("request", request);
		memory.put("answer", answer);
		memory.put("contextCharacters", context.characterCount());
		memory.put("suggestionIds", suggestions.stream()
				.map(AgentSuggestionResponse::suggestionId)
				.toList());
		try {
			return objectMapper.writeValueAsString(memory);
		} catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize room memory summary.", exception);
		}
	}
}
