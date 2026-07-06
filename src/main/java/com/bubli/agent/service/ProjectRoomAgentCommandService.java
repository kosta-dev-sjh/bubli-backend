package com.bubli.agent.service;

import com.bubli.agent.dto.AgentSuggestionResponse;
import com.bubli.agent.dto.ProjectRoomAgentCommandResponse;
import com.bubli.agent.dto.ProjectRoomRagContext;
import com.bubli.agent.model.AiCallExecutor;
import com.bubli.agent.type.AgentCommandMode;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.chat.dto.ChatMessageResponse;
import com.bubli.chat.service.ChatMessagePublicService;
import com.bubli.global.locale.SupportedLocale;
import com.bubli.memory.dto.RoomMemorySummaryContextResult;
import com.bubli.memory.service.RoomMemoryPublicService;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.project.service.ProjectRoomEventPublicService;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.dto.ResourceSearchHit;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.user.service.UserLocalePublicService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectRoomAgentCommandService {

	private static final String PROMPT_VERSION = "project-room-agent-command-rag-source-only-v1";

	private final ProjectMembershipPublicService projectMembershipPublicService;
	private final ChatMessagePublicService chatMessagePublicService;
	private final RoomMemoryPublicService roomMemoryPublicService;
	private final AgentSuggestionCommandService agentSuggestionCommandService;
	private final ProjectRoomEventPublicService projectRoomEventPublicService;
	private final UserLocalePublicService userLocalePublicService;
	private final ResourcePublicService resourcePublicService;
	private final ProjectRoomRagGroundingService ragGroundingService;
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
		ResourceInventoryAnswer inventoryAnswer = resourceInventoryAnswer(userId, roomId, message, locale);
		if (inventoryAnswer != null) {
			return persistResponse(
					userId,
					roomId,
					message,
					commandMode,
					inventoryAnswer.answer(),
					List.of(),
					ProjectRoomRagContext.ungrounded(),
					inventoryAnswer.resources()
			);
		}
		ProjectRoomRagContext ragContext = ragGroundingService.retrieve(userId, roomId, message, locale, commandMode);
		String answer = answer(message, commandMode, locale, ragContext);
		List<AgentSuggestionResponse> suggestions = createSuggestions(
				userId,
				roomId,
				message,
				commandMode,
				answer,
				ragContext
		);
		return persistResponse(userId, roomId, message, commandMode, answer, suggestions, ragContext, List.of());
	}

	private ProjectRoomAgentCommandResponse persistResponse(
			UUID userId,
			UUID roomId,
			String message,
			AgentCommandMode commandMode,
			String answer,
			List<AgentSuggestionResponse> suggestions,
			ProjectRoomRagContext ragContext,
			List<ResourceResult> metadataResources
	) {
		UUID responseResourceId = metadataResources.isEmpty() ? ragContext.firstResourceId() : metadataResources.getFirst().id();
		ChatMessageResponse chatMessage = ChatMessageResponse.from(chatMessagePublicService.createRoomAgentResponse(
				userId,
				roomId,
				responseBody(message, commandMode, answer, suggestions, ragContext, metadataResources),
				responseResourceId
		));
		RoomMemorySummaryContextResult memory = roomMemoryPublicService.createDraft(
				userId,
				roomId,
				chatMessage.roomSequence(),
				chatMessage.roomSequence(),
				memoryJson(message, commandMode, answer, suggestions, ragContext, metadataResources)
		);
		return new ProjectRoomAgentCommandResponse(chatMessage, memory, suggestions);
	}

	private ResourceInventoryAnswer resourceInventoryAnswer(UUID userId, UUID roomId, String message, String locale) {
		if (!isResourceInventoryRequest(message)) {
			return null;
		}
		List<ResourceResult> resources = resourcePublicService.getRecentRoomResources(userId, roomId, 10);
		if (resources.isEmpty()) {
			return new ResourceInventoryAnswer(resources, noUploadedResourcesAnswer(locale));
		}
		return new ResourceInventoryAnswer(resources, uploadedResourcesAnswer(resources, locale));
	}

	private boolean isResourceInventoryRequest(String message) {
		String normalized = normalize(message);
		return containsAny(normalized, "업로드", "올라온", "파일", "자료", "문서", "resource", "file", "upload",
				"資料", "文書", "ファイル", "アップロード", "アップロード済み", "登録", "添付")
				&& containsAny(normalized, "무엇", "뭐", "목록", "리스트", "보여", "알려", "현재", "있", "what", "list",
				"show", "which", "?", "？", "何", "どんな", "どの", "一覧", "教え", "見せて", "表示", "ある", "あります");
	}

	private String uploadedResourcesAnswer(List<ResourceResult> resources, String locale) {
		String lines = resources.stream()
				.map(resource -> "- %s (%s, %s)".formatted(resource.title(), resource.kind(), resource.status()))
				.reduce((left, right) -> left + "\n" + right)
				.orElse("");
		if ("en-US".equals(locale)) {
			return "The current project room has these uploaded resources:\n%s".formatted(lines);
		}
		if ("ja-JP".equals(locale)) {
			return "現在のプロジェクトルームにアップロードされている資料は次のとおりです。\n%s".formatted(lines);
		}
		return "현재 프로젝트룸에 업로드된 자료는 다음과 같습니다.\n%s".formatted(lines);
	}

	private String noUploadedResourcesAnswer(String locale) {
		if ("en-US".equals(locale)) {
			return "No uploaded resources were found in the current project room.";
		}
		if ("ja-JP".equals(locale)) {
			return "現在のプロジェクトルームでアップロード済みの資料は見つかりませんでした。";
		}
		return "현재 프로젝트룸에서 업로드된 자료를 찾지 못했습니다.";
	}

	private String answer(String message, AgentCommandMode mode, String locale, ProjectRoomRagContext ragContext) {
		if (!ragContext.grounded()) {
			return noAnswer(locale);
		}
		ChatModel chatModel = chatModelProvider.getIfAvailable();
		if (chatModel == null) {
			return noAnswer(locale);
		}
		AiCallExecutor executor = aiCallExecutorProvider.getIfAvailable();
		String prompt = prompt(message, mode, locale, ragContext);
		try {
			if (executor == null) {
				return chatModel.call(prompt);
			}
			return executor.execute("project-room-agent-command-rag", () -> chatModel.call(prompt));
		} catch (RuntimeException exception) {
			log.warn("Project room RAG LLM answer failed.", exception);
			return noAnswer(locale);
		}
	}

	private String prompt(String message, AgentCommandMode mode, String locale, ProjectRoomRagContext ragContext) {
		return """
				You are Bubli's project room agent. %s
				Mode: %s

				Use ONLY the project material sources listed under "Retrieved project document chunks".
				Do not use recent chat history, room memory summaries, tasks, WBS, schedules, user memory, general world knowledge, or assumptions as factual evidence.
				If the retrieved chunks do not contain enough information to answer, reply exactly with this sentence in the response language: %s
				For SUGGEST mode, produce TODO, TASK, WBS, REQUIREMENT, QUESTION, or REVIEW_ITEM candidates only from the retrieved chunks.
				Keep source names and direct evidence in the original language, but write user-facing explanation in the requested response language.

				User message:
				%s

				Retrieved project document chunks:
				%s
				""".formatted(
				languageInstruction(locale),
				mode,
				noAnswer(locale),
				message,
				ragContext.promptBlock()
		);
	}

	private String noAnswer(String locale) {
		return switch (locale) {
			case "en-US" -> "I cannot determine that from the project materials.";
			case "ja-JP" -> "プロジェクト資料の範囲では分かりません。";
			default -> "프로젝트 자료 기준에서는 알 수 없는 내용입니다.";
		};
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
			ProjectRoomRagContext ragContext
	) {
		if (mode != AgentCommandMode.SUGGEST || !ragContext.grounded()) {
			return List.of();
		}
		AgentSuggestionType suggestionType = inferSuggestionType(message);
		AgentSuggestionResponse suggestion = agentSuggestionCommandService.createDraft(
				userId,
				roomId,
				null,
				ragContext.firstResourceId(),
				suggestionType,
				suggestionPayload(suggestionType, message, answer),
				suggestionEvidence(ragContext)
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
		if (containsAny(normalized, "요구사항", "요구", "requirement", "要件")) {
			return AgentSuggestionType.REQUIREMENT;
		}
		if (containsAny(normalized, "?", "질문", "확인", "물어", "문의", "누락", "불명확",
				"question", "ask", "unclear", "missing", "質問", "確認", "不明")) {
			return AgentSuggestionType.QUESTION;
		}
		if (containsAny(normalized, "검토", "리뷰", "위험", "리스크", "이슈", "조건", "계약", "확인 필요",
				"review", "risk", "issue", "condition", "contract", "契約", "リスク", "条件")) {
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
		payload.put("ragGrounded", true);
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

	private Map<String, Object> suggestionEvidence(ProjectRoomRagContext ragContext) {
		Map<String, Object> evidence = new LinkedHashMap<>();
		evidence.put("source", "PROJECT_ROOM_AGENT_COMMAND");
		evidence.put("promptVersion", PROMPT_VERSION);
		evidence.put("ragGrounded", ragContext.grounded());
		evidence.put("ragMaxSimilarity", ragContext.maxSimilarity());
		evidence.put("resourceIds", ragContext.resourceIds());
		evidence.put("ragHits", ragHits(ragContext));
		return evidence;
	}

	private JsonNode responseBody(
			String request,
			AgentCommandMode mode,
			String answer,
			List<AgentSuggestionResponse> suggestions,
			ProjectRoomRagContext ragContext,
			List<ResourceResult> metadataResources
	) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("text", answer);
		body.put("request", request);
		body.put("mode", mode.name());
		body.put("promptVersion", PROMPT_VERSION);
		body.put("contextCharacters", ragContext.promptBlock().length());
		body.put("suggestionIds", suggestions.stream()
				.map(AgentSuggestionResponse::suggestionId)
				.toList());
		body.put("ragGrounded", ragContext.grounded());
		body.put("ragMaxSimilarity", ragContext.maxSimilarity());
		body.put("ragHits", ragHits(ragContext));
		body.put("resourceIds", ragContext.resourceIds());
		if (!metadataResources.isEmpty()) {
			body.put("resources", metadataResources.stream().map(this::resourcePayload).toList());
			body.put("resourceIds", metadataResources.stream().map(ResourceResult::id).toList());
		}
		return objectMapper.valueToTree(body);
	}

	private String memoryJson(
			String request,
			AgentCommandMode mode,
			String answer,
			List<AgentSuggestionResponse> suggestions,
			ProjectRoomRagContext ragContext,
			List<ResourceResult> metadataResources
	) {
		Map<String, Object> memory = new LinkedHashMap<>();
		memory.put("source", "PROJECT_ROOM_AGENT_COMMAND");
		memory.put("mode", mode.name());
		memory.put("request", request);
		memory.put("answer", answer);
		memory.put("contextCharacters", ragContext.promptBlock().length());
		memory.put("suggestionIds", suggestions.stream()
				.map(AgentSuggestionResponse::suggestionId)
				.toList());
		memory.put("ragGrounded", ragContext.grounded());
		memory.put("ragMaxSimilarity", ragContext.maxSimilarity());
		memory.put("ragHits", ragHits(ragContext));
		memory.put("resourceIds", ragContext.resourceIds());
		if (!metadataResources.isEmpty()) {
			memory.put("resources", metadataResources.stream().map(this::resourcePayload).toList());
			memory.put("resourceIds", metadataResources.stream().map(ResourceResult::id).toList());
		}
		try {
			return objectMapper.writeValueAsString(memory);
		} catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize room memory summary.", exception);
		}
	}

	private List<Map<String, Object>> ragHits(ProjectRoomRagContext ragContext) {
		return ragContext.hits().stream()
				.map(this::ragHit)
				.toList();
	}

	private Map<String, Object> ragHit(ResourceSearchHit hit) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("resourceId", hit.resourceId());
		payload.put("chunkIndex", hit.chunkIndex());
		payload.put("pageNumber", hit.pageNumber());
		payload.put("similarityScore", hit.similarityScore());
		return payload;
	}

	private Map<String, Object> resourcePayload(ResourceResult resource) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("resourceId", resource.id());
		payload.put("title", resource.title());
		payload.put("kind", resource.kind().name());
		payload.put("visibility", resource.visibility().name());
		payload.put("status", resource.status().name());
		payload.put("createdAt", resource.createdAt() == null ? null : resource.createdAt().toString());
		return payload;
	}

	private record ResourceInventoryAnswer(
			List<ResourceResult> resources,
			String answer
	) {
	}
}
