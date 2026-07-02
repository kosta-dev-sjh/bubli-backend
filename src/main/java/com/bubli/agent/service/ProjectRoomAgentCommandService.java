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
import com.bubli.global.locale.SupportedLocale;
import com.bubli.memory.dto.RoomMemorySummaryContextResult;
import com.bubli.memory.service.RoomMemoryPublicService;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.project.service.ProjectRoomEventPublicService;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.user.service.UserLocalePublicService;
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
import java.util.Optional;
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
	private final ResourcePublicService resourcePublicService;
	private final UserLocalePublicService userLocalePublicService;
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
		String locale = SupportedLocale.normalize(userLocalePublicService.resolveLocaleCode(userId, null));
		ResourceLookup resourceLookup = resolveResourceLookup(userId, roomId, message);
		AgentJobContext context = contextCollector.collect(new AgentJobQueueMessage(
				UUID.randomUUID(),
				userId,
				roomId,
				resourceIds == null || resourceIds.isEmpty() ? null : resourceIds.getFirst(),
				AgentJobType.GENERATE_QUESTIONS,
				Map.of(
						"source", "PROJECT_ROOM_AGENT_COMMAND",
						"mode", commandMode.name(),
						"locale", locale,
						"message", message,
						"resourceIds", resourceIds == null ? List.of() : resourceIds
				),
				java.time.Instant.now()
		));
		String answer = answer(message, commandMode, context, locale, resourceLookup);
		List<AgentSuggestionResponse> suggestions = createSuggestions(
				userId,
				roomId,
				message,
				commandMode,
				answer,
				context,
				resourceIds
		);
		UUID responseResourceId = responseResourceId(resourceIds, resourceLookup.resource());
		ChatMessageResponse chatMessage = ChatMessageResponse.from(chatMessagePublicService.createRoomAgentResponse(
				userId,
				roomId,
				responseBody(message, commandMode, answer, context, suggestions, resourceLookup),
				responseResourceId
		));
		RoomMemorySummaryContextResult memory = roomMemoryPublicService.createDraft(
				userId,
				roomId,
				chatMessage.roomSequence(),
				chatMessage.roomSequence(),
				memoryJson(message, commandMode, answer, context, suggestions, resourceLookup)
		);
		return new ProjectRoomAgentCommandResponse(chatMessage, memory, suggestions);
	}

	private ResourceLookup resolveResourceLookup(UUID userId, UUID roomId, String message) {
		if (isLatestRoomFileRequest(message)) {
			return new ResourceLookup(true, List.of("file"), resourcePublicService.findLatestRoomFile(userId, roomId));
		}
		List<String> keywords = resourceKeywords(message);
		if (keywords.isEmpty()) {
			return ResourceLookup.notRequested();
		}
		return new ResourceLookup(true, keywords, resourcePublicService.findLatestRoomResource(userId, roomId, keywords));
	}

	private boolean isLatestRoomFileRequest(String message) {
		String normalized = normalize(message);
		return containsAny(normalized, "최근", "마지막", "최신", "latest", "newest", "last")
				&& containsAny(normalized, "파일", "업로드", "file", "upload");
	}

	private List<String> resourceKeywords(String message) {
		String normalized = normalize(message);
		if (!containsAny(normalized, "최근", "마지막", "최신", "latest", "newest", "last", "直近", "最新")) {
			return List.of();
		}
		if (containsAny(normalized, "계약", "contract", "agreement", "契約")) {
			return List.of("계약", "contract", "agreement");
		}
		if (containsAny(normalized, "견적", "estimate", "quotation", "quote", "見積")) {
			return List.of("견적", "estimate", "quotation");
		}
		if (containsAny(normalized, "발주", "purchase order", "po", "注文")) {
			return List.of("발주", "purchase", "order");
		}
		if (containsAny(normalized, "요구사항", "요구", "requirements", "requirement", "要件")) {
			return List.of("요구사항", "requirement", "requirements");
		}
		if (containsAny(normalized, "회의록", "회의", "meeting minutes", "minutes", "議事録")) {
			return List.of("회의록", "meeting", "minutes");
		}
		if (containsAny(normalized, "참고자료", "참고", "reference", "参考")) {
			return List.of("참고", "reference", "reference");
		}
		if (containsAny(normalized, "일반자료", "일반", "general")) {
			return List.of("일반", "general", "general");
		}
		if (containsAny(normalized, "자료", "문서", "resource", "document", "file")) {
			return List.of("자료", "문서", "document");
		}
		return List.of();
	}

	private UUID responseResourceId(List<UUID> resourceIds, Optional<ResourceResult> resource) {
		if (resourceIds != null && !resourceIds.isEmpty()) {
			return resourceIds.getFirst();
		}
		return resource.map(ResourceResult::id).orElse(null);
	}

	private String answer(
			String message,
			AgentCommandMode mode,
			AgentJobContext context,
			String locale,
			ResourceLookup resourceLookup
	) {
		ChatModel chatModel = chatModelProvider.getIfAvailable();
		if (chatModel == null) {
			return fallbackAnswer(message, mode, context, locale, resourceLookup);
		}
		AiCallExecutor executor = aiCallExecutorProvider.getIfAvailable();
		String prompt = prompt(message, mode, context, locale, resourceLookup);
		if (executor == null) {
			return chatModel.call(prompt);
		}
		return executor.execute("project-room-agent-command", () -> chatModel.call(prompt));
	}

	private String prompt(
			String message,
			AgentCommandMode mode,
			AgentJobContext context,
			String locale,
			ResourceLookup resourceLookup
	) {
		return """
				You are Bubli's project room agent. %s
				Mode: %s
				Do not invent confirmed facts. Use the provided project context and say what should be checked when context is insufficient.
				You may answer document questions, extract business facts, or create suggestion candidates such as TODO, TASK, WBS, REQUIREMENT, QUESTION, and REVIEW_ITEM.

				User message:
				%s

				Resolved resource context:
				%s

				Project context:
				%s
				""".formatted(languageInstruction(locale), mode, message, resourceContext(resourceLookup), context.promptBlock());
	}

	private String resourceContext(ResourceLookup resourceLookup) {
		if (!resourceLookup.requested()) {
			return "No explicit latest resource lookup was requested.";
		}
		return resourceLookup.resource()
				.map(resource -> "Latest resource candidate: id=%s, title=%s, createdAt=%s, keywords=%s"
						.formatted(resource.id(), resource.title(), resource.createdAt(), resourceLookup.keywords()))
				.orElse("Latest resource lookup requested but no matching resource was found. keywords=%s"
						.formatted(resourceLookup.keywords()));
	}

	private String fallbackAnswer(
			String message,
			AgentCommandMode mode,
			AgentJobContext context,
			String locale,
			ResourceLookup resourceLookup
	) {
		if (resourceLookup.requested()) {
			return resourceLookup.resource()
					.map(resource -> latestResourceFoundAnswer(resource, locale))
					.orElseGet(() -> latestResourceNotFoundAnswer(locale));
		}
		if ("en-US".equals(locale)) {
			return switch (mode) {
				case ANSWER -> "I checked the currently collected project context. Request: %s".formatted(message);
				case SUMMARIZE -> "Current project context summary: %s".formatted(context.promptBlock());
				case SUGGEST -> "Create a TODO, WBS, requirement, question, or review item from the request and confirm it through the approval flow.";
			};
		}
		if ("ja-JP".equals(locale)) {
			return switch (mode) {
				case ANSWER -> "現在収集されているプロジェクト文脈を確認しました。リクエスト: %s".formatted(message);
				case SUMMARIZE -> "現在のプロジェクト文脈の要約です: %s".formatted(context.promptBlock());
				case SUGGEST -> "リクエスト内容をもとにTODO、WBS、要件、質問、またはレビュー項目を作成し、承認フローで確定してください。";
			};
		}
		return switch (mode) {
			case ANSWER -> "현재 수집된 프로젝트 맥락을 기준으로 확인했습니다. 요청: %s".formatted(message);
			case SUMMARIZE -> "현재 프로젝트 맥락 요약입니다: %s".formatted(context.promptBlock());
			case SUGGEST -> "요청 내용을 기준으로 TODO, WBS, 요구사항, 질문 또는 검토 항목을 생성하고 승인 흐름에서 확정하세요.";
		};
	}

	private String latestResourceFoundAnswer(ResourceResult resource, String locale) {
		if ("en-US".equals(locale)) {
			return "The most recent matching resource appears to be \"%s\".".formatted(resource.title());
		}
		if ("ja-JP".equals(locale)) {
			return "最も最近アップロードされた該当資料は「%s」と判断されます。".formatted(resource.title());
		}
		return "가장 최근에 올라온 관련 자료는 \"%s\"입니다.".formatted(resource.title());
	}

	private String latestResourceNotFoundAnswer(String locale) {
		if ("en-US".equals(locale)) {
			return "I could not find a matching resource in the current project room.";
		}
		if ("ja-JP".equals(locale)) {
			return "現在のプロジェクトルームで該当する資料を見つけられませんでした。";
		}
		return "현재 프로젝트룸에서 조건에 맞는 자료를 찾지 못했습니다.";
	}

	private String languageInstruction(String locale) {
		return switch (locale) {
			case "en-US" -> "Write a concise natural English response.";
			case "ja-JP" -> "Write a concise natural Japanese response.";
			default -> "Write a concise natural Korean response.";
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
		String normalized = normalize(message);
		if (containsAny(normalized, "wbs", "work breakdown")) {
			return AgentSuggestionType.WBS;
		}
		if (containsAny(normalized, "작업", "태스크", "task")) {
			return AgentSuggestionType.TASK;
		}
		if (containsAny(normalized, "todo", "할 일", "할일", "to-do")) {
			return AgentSuggestionType.TODO;
		}
		if (containsAny(normalized, "요구사항", "요구", "requirement")) {
			return AgentSuggestionType.REQUIREMENT;
		}
		if (containsAny(normalized, "?", "질문", "확인", "물어", "문의", "누락", "불명확",
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
			if (value.contains(candidate.toLowerCase(Locale.ROOT))) {
				return true;
			}
		}
		return false;
	}

	private String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
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
				case WBS -> "WBS 후보";
				case TASK -> "작업 후보";
				case REQUIREMENT -> "요구사항 후보";
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
			List<AgentSuggestionResponse> suggestions,
			ResourceLookup resourceLookup
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
		resourceLookup.resource().ifPresent(resource -> body.put("resources", List.of(resourcePayload(resource))));
		return objectMapper.valueToTree(body);
	}

	private String memoryJson(
			String request,
			AgentCommandMode mode,
			String answer,
			AgentJobContext context,
			List<AgentSuggestionResponse> suggestions,
			ResourceLookup resourceLookup
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
		resourceLookup.resource().ifPresent(resource -> memory.put("resources", List.of(resourcePayload(resource))));
		try {
			return objectMapper.writeValueAsString(memory);
		} catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize room memory summary.", exception);
		}
	}

	private Map<String, Object> resourcePayload(ResourceResult resource) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("resourceId", resource.id());
		payload.put("title", resource.title());
		payload.put("createdAt", resource.createdAt() == null ? null : resource.createdAt().toString());
		return payload;
	}

	private record ResourceLookup(
			boolean requested,
			List<String> keywords,
			Optional<ResourceResult> resource
	) {
		private static ResourceLookup notRequested() {
			return new ResourceLookup(false, List.of(), Optional.empty());
		}
	}
}
