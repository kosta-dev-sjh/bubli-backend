package com.bubli.agent.service;

import com.bubli.agent.dto.AgentSuggestionResponse;
import com.bubli.agent.dto.ProjectRoomAgentCommandResponse;
import com.bubli.agent.dto.ProjectRoomGroundingContext;
import com.bubli.agent.dto.ProjectRoomGroundingEvidence;
import com.bubli.agent.dto.ProjectRoomGroundingSourceType;
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
import com.bubli.user.dto.UserResult;
import com.bubli.user.service.UserLocalePublicService;
import com.bubli.user.service.UserPublicService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectRoomAgentCommandService {

	private static final String PROMPT_VERSION = "project-room-agent-command-project-grounded-v2";
	private static final String FALLBACK_AMBIGUOUS_RESOURCE_INTENT = "AMBIGUOUS_RESOURCE_INTENT";

	private final ProjectMembershipPublicService projectMembershipPublicService;
	private final ChatMessagePublicService chatMessagePublicService;
	private final RoomMemoryPublicService roomMemoryPublicService;
	private final AgentSuggestionCommandService agentSuggestionCommandService;
	private final ProjectRoomEventPublicService projectRoomEventPublicService;
	private final UserLocalePublicService userLocalePublicService;
	private final UserPublicService userPublicService;
	private final ResourcePublicService resourcePublicService;
	private final ProjectRoomGroundingService groundingService;
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
		String clarificationAnswer = ambiguousResourceAnswer(message, locale);
		if (clarificationAnswer != null) {
			return persistResponse(userId, roomId, message, commandMode, clarificationAnswer,
					FALLBACK_AMBIGUOUS_RESOURCE_INTENT, List.of(), ProjectRoomGroundingContext.ungrounded(), List.of());
		}
		ResourceInventoryAnswer inventoryAnswer = resourceInventoryAnswer(userId, roomId, message, locale);
		if (inventoryAnswer != null) {
			return persistResponse(userId, roomId, message, commandMode, inventoryAnswer.answer(), null,
					List.of(), ProjectRoomGroundingContext.ungrounded(), inventoryAnswer.resources());
		}
		ProjectRoomGroundingContext groundingContext = groundingService.retrieve(userId, roomId, message, locale, commandMode);
		AnswerResult answer = answer(message, commandMode, locale, groundingContext);
		List<AgentSuggestionResponse> suggestions = createSuggestions(
				userId,
				roomId,
				message,
				commandMode,
				answer.text(),
				groundingContext
		);
		return persistResponse(userId, roomId, message, commandMode, answer.text(), answer.fallbackReason(),
				suggestions, groundingContext, List.of());
	}

	private ProjectRoomAgentCommandResponse persistResponse(
			UUID userId,
			UUID roomId,
			String message,
			AgentCommandMode commandMode,
			String answer,
			String fallbackReason,
			List<AgentSuggestionResponse> suggestions,
			ProjectRoomGroundingContext groundingContext,
			List<ResourceResult> metadataResources
	) {
		UUID responseResourceId = metadataResources.isEmpty()
				? groundingContext.firstResourceId()
				: metadataResources.getFirst().id();
		UserResult requester = userPublicService.getUser(userId);
		JsonNode body = responseBody(
				requester,
				message,
				commandMode,
				answer,
				fallbackReason,
				suggestions,
				groundingContext,
				metadataResources
		);
		ChatMessageResponse chatMessage = ChatMessageResponse.from(chatMessagePublicService.createRoomAgentResponse(
				userId,
				roomId,
				body,
				responseResourceId
		));
		RoomMemorySummaryContextResult memory = roomMemoryPublicService.createDraft(
				userId,
				roomId,
				chatMessage.roomSequence(),
				chatMessage.roomSequence(),
				memoryJson(message, commandMode, answer, fallbackReason, suggestions, groundingContext, metadataResources)
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

	private String ambiguousResourceAnswer(String message, String locale) {
		if (!isAmbiguousResourceRequest(message)) {
			return null;
		}
		return switch (locale) {
			case "en-US" -> "Do you want the uploaded file list, or should I summarize a file's contents?";
			case "ja-JP" -> "アップロード済みファイルの一覧が必要ですか？それとも特定ファイルの内容を要約しますか？";
			default -> "업로드된 파일 목록을 원하시나요, 아니면 특정 파일의 내용을 요약할까요?";
		};
	}

	private boolean isAmbiguousResourceRequest(String message) {
		String normalized = normalize(message);
		if (isResourceContentRequest(normalized)) {
			return false;
		}
		return hasResourceTerm(normalized)
				&& containsAny(normalized, "알려", "말해", "설명", "요약", "tell", "explain", "summarize", "教えて", "説明", "要約")
				&& !hasInventoryIntent(normalized);
	}

	private boolean isResourceInventoryRequest(String message) {
		String normalized = normalize(message);
		if (isResourceContentRequest(normalized)) {
			return false;
		}
		return hasResourceTerm(normalized) && hasInventoryIntent(normalized);
	}

	private boolean hasResourceTerm(String normalized) {
		return containsAny(normalized,
				"업로드", "올린", "파일", "자료", "문서", "resource", "file", "upload", "document",
				"アップロード", "ファイル", "資料", "文書");
	}

	private boolean hasInventoryIntent(String normalized) {
		return containsAny(normalized,
				"무엇", "뭐", "목록", "리스트", "보여", "현재", "어떤 파일", "업로드된 파일",
				"what", "list", "show", "which",
				"何", "一覧", "リスト", "見せて", "どのファイル", "どんなファイル", "ある", "アップロード済み");
	}

	private boolean isResourceContentRequest(String normalized) {
		return AgentQuerySupport.hasRequirementIdentifier(normalized)
				|| containsAny(normalized,
				"내용", "뜻", "요약", "정리", "분석", "설명", "주요", "무슨 내용", "어떤 내용",
				"기능", "요구사항", "요구 사항", "요건", "말하", "어떤 것을", "어떤것", "무슨 기능", "어떤 기능",
				"summary", "summarize", "summarise", "content", "key point", "main point", "explain",
				"requirement", "feature", "means", "refer to",
				"内容", "意味", "要約", "整理", "分析", "説明", "主な", "機能", "要件", "何を指す");
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
			return "現在のプロジェクトルームには、次のアップロード済み資料があります:\n%s".formatted(lines);
		}
		return "현재 프로젝트룸에 업로드된 자료는 다음과 같습니다.\n%s".formatted(lines);
	}

	private String noUploadedResourcesAnswer(String locale) {
		if ("en-US".equals(locale)) {
			return "No uploaded resources were found in the current project room.";
		}
		if ("ja-JP".equals(locale)) {
			return "現在のプロジェクトルームにアップロード済み資料は見つかりませんでした。";
		}
		return "현재 프로젝트룸에서 업로드된 자료를 찾지 못했습니다.";
	}

	private AnswerResult answer(String message, AgentCommandMode mode, String locale, ProjectRoomGroundingContext groundingContext) {
		if (!groundingContext.grounded()) {
			if (groundingContext.retrievalFailed()) {
				return new AnswerResult(searchFailureAnswer(locale), "GROUNDING_RETRIEVAL_FAILED");
			}
			return new AnswerResult(noAnswer(locale), "NO_GROUNDING");
		}
		ChatModel chatModel = chatModelProvider.getIfAvailable();
		if (chatModel == null) {
			return new AnswerResult(noAnswer(locale), "NO_CHAT_MODEL");
		}
		AiCallExecutor executor = aiCallExecutorProvider.getIfAvailable();
		String prompt = prompt(message, mode, locale, groundingContext);
		try {
			String rawAnswer = executor == null
					? chatModel.call(prompt)
					: executor.execute("project-room-agent-command-grounded", () -> chatModel.call(prompt));
			return new AnswerResult(AgentQuerySupport.removeAppendedNoAnswer(rawAnswer, noAnswer(locale)), null);
		} catch (RuntimeException exception) {
			log.warn("Project room RAG LLM answer failed.", exception);
			return new AnswerResult(noAnswer(locale), "LLM_FAILED");
		}
	}

	private String prompt(String message, AgentCommandMode mode, String locale, ProjectRoomGroundingContext groundingContext) {
		return """
				You are Bubli's project room agent. %s
				Mode: %s

				Use ONLY the project documents and management data listed under "Retrieved project grounding sources".
				Do not use recent chat history, room memory summaries, user memory, user profile memory, general world knowledge, or assumptions as factual evidence.
				If at least one retrieved source is relevant, answer from the available evidence even when it is partial.
				When the evidence is partial, separate confirmed facts from missing or unclear items.
				Prefer sources with higher fusionScore and matchReason values such as REQUIREMENT_ID_MATCH, QUOTED_PHRASE_MATCH, KEYWORD_MATCH, or TITLE_MATCHED_RESOURCE.
				If the highest ranked sources still do not support the user's question, use the no-answer sentence instead of guessing.
				Never append the no-answer sentence after a useful partial answer.
				Use this exact no-answer sentence only when none of the retrieved sources are relevant at all: %s
				For SUGGEST mode, produce TODO, TASK, WBS, REQUIREMENT, QUESTION, or REVIEW_ITEM candidates only from the retrieved sources.
				For SUGGEST mode, write each candidate on its own short bullet line when multiple candidates are useful.
				Keep source names and direct evidence in the original language, but write user-facing explanation in the requested response language.
				Do not print raw retrieval blocks, resourceId values, retrievalMode values, or metadata lines in the answer body.

				User message:
				%s

				Retrieved project grounding sources:
				%s
				""".formatted(languageInstruction(locale), mode, noAnswer(locale), message, groundingContext.promptBlock());
	}

	private String noAnswer(String locale) {
		return switch (locale) {
			case "en-US" -> "I cannot determine that from the project documents or management data.";
			case "ja-JP" -> "プロジェクト資料および管理データの範囲では分かりません。";
			default -> "프로젝트 문서 및 관리 데이터 기준에서는 알 수 없는 내용입니다.";
		};
	}

	private String searchFailureAnswer(String locale) {
		return switch (locale) {
			case "en-US" -> "A temporary problem occurred while searching the project evidence. Please try again shortly.";
			case "ja-JP" -> "プロジェクト根拠の検索中に一時的な問題が発生しました。しばらくしてからもう一度お試しください。";
			default -> "프로젝트 근거 검색 중 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.";
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
			ProjectRoomGroundingContext groundingContext
	) {
		if (mode != AgentCommandMode.SUGGEST || !groundingContext.grounded()) {
			return List.of();
		}
		AgentSuggestionType suggestionType = inferSuggestionType(message);
		List<String> items = AgentQuerySupport.suggestionItems(answer, noAnswer("ko-KR"), 5);
		if (items.isEmpty()) {
			items = List.of(message);
		}
		List<AgentSuggestionResponse> suggestions = new ArrayList<>();
		for (String item : items) {
			suggestions.add(agentSuggestionCommandService.createDraft(
					userId,
					roomId,
					null,
					groundingContext.firstResourceId(),
					suggestionType,
					suggestionPayload(suggestionType, message, item, answer, groundingContext),
					suggestionEvidence(groundingContext)
			));
		}
		projectRoomEventPublicService.recordAgentSuggestionsCreated(
				userId,
				roomId,
				suggestions.stream().map(AgentSuggestionResponse::suggestionId).toList(),
				suggestions.stream().map(suggestion -> suggestion.suggestionType().name()).toList()
		);
		return suggestions;
	}

	private AgentSuggestionType inferSuggestionType(String message) {
		String normalized = normalize(message);
		if (containsAny(normalized, "wbs", "work breakdown")) {
			return AgentSuggestionType.WBS;
		}
		if (containsAny(normalized, "작업", "태스크", "task", "タスク")) {
			return AgentSuggestionType.TASK;
		}
		if (containsAny(normalized, "todo", "할일", "to-do")) {
			return AgentSuggestionType.TODO;
		}
		if (containsAny(normalized, "요구사항", "요구", "requirement", "要件")) {
			return AgentSuggestionType.REQUIREMENT;
		}
		if (containsAny(normalized, "?", "질문", "확인", "물어", "문의", "연락", "불명확",
				"question", "ask", "unclear", "missing", "質問", "確認", "不明")) {
			return AgentSuggestionType.QUESTION;
		}
		if (containsAny(normalized, "검토", "리뷰", "위험", "리스크", "이슈", "조건", "계약", "확인 필요",
				"review", "risk", "issue", "condition", "contract", "レビュー", "リスク", "課題")) {
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

	private Map<String, Object> suggestionPayload(
			AgentSuggestionType suggestionType,
			String message,
			String item,
			String answer,
			ProjectRoomGroundingContext groundingContext
	) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", suggestionType.name());
		payload.put("title", suggestionTitle(item, suggestionType));
		payload.put("description", item);
		payload.put("request", message);
		payload.put("agentResponse", answer);
		payload.put("source", "PROJECT_ROOM_AGENT_COMMAND");
		payload.put("grounded", true);
		payload.put("sourceTypes", sourceTypes(groundingContext));
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

	private Map<String, Object> suggestionEvidence(ProjectRoomGroundingContext groundingContext) {
		Map<String, Object> evidence = new LinkedHashMap<>();
		evidence.put("source", "PROJECT_ROOM_AGENT_COMMAND");
		evidence.put("promptVersion", PROMPT_VERSION);
		putGroundingMetadata(evidence, groundingContext);
		return evidence;
	}

	private JsonNode responseBody(
			UserResult requester,
			String request,
			AgentCommandMode mode,
			String answer,
			String fallbackReason,
			List<AgentSuggestionResponse> suggestions,
			ProjectRoomGroundingContext groundingContext,
			List<ResourceResult> metadataResources
	) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("text", answer);
		body.put("request", request);
		body.put("requesterId", requester.id());
		body.put("requesterName", requester.name());
		body.put("mode", mode.name());
		body.put("promptVersion", PROMPT_VERSION);
		body.put("contextCharacters", groundingContext.promptBlock().length());
		body.put("answerCompleteness", metadataResources.isEmpty()
				? answerCompleteness(answer, fallbackReason, groundingContext)
				: "ANSWERED");
		body.put("fallbackReason", fallbackReason);
		body.put("missingInfo", metadataResources.isEmpty()
				? missingInfo(request, answer, fallbackReason, groundingContext)
				: List.of());
		body.put("suggestionIds", suggestions.stream().map(AgentSuggestionResponse::suggestionId).toList());
		putGroundingMetadata(body, groundingContext);
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
			String fallbackReason,
			List<AgentSuggestionResponse> suggestions,
			ProjectRoomGroundingContext groundingContext,
			List<ResourceResult> metadataResources
	) {
		Map<String, Object> memory = new LinkedHashMap<>();
		memory.put("source", "PROJECT_ROOM_AGENT_COMMAND");
		memory.put("mode", mode.name());
		memory.put("request", request);
		memory.put("answer", answer);
		memory.put("contextCharacters", groundingContext.promptBlock().length());
		memory.put("answerCompleteness", metadataResources.isEmpty()
				? answerCompleteness(answer, fallbackReason, groundingContext)
				: "ANSWERED");
		memory.put("fallbackReason", fallbackReason);
		memory.put("missingInfo", metadataResources.isEmpty()
				? missingInfo(request, answer, fallbackReason, groundingContext)
				: List.of());
		memory.put("suggestionIds", suggestions.stream().map(AgentSuggestionResponse::suggestionId).toList());
		putGroundingMetadata(memory, groundingContext);
		if (!metadataResources.isEmpty()) {
			memory.put("resources", metadataResources.stream().map(this::resourcePayload).toList());
			memory.put("resourceIds", metadataResources.stream().map(ResourceResult::id).toList());
		}
		try {
			return objectMapper.writeValueAsString(memory);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize room memory summary.", exception);
		}
	}

	private void putGroundingMetadata(Map<String, Object> payload, ProjectRoomGroundingContext groundingContext) {
		payload.put("grounded", groundingContext.grounded());
		payload.put("retrievalFailed", groundingContext.retrievalFailed());
		payload.put("retrievalFailureReason", groundingContext.retrievalFailureReason());
		payload.put("sourceTypes", sourceTypes(groundingContext));
		payload.put("evidenceItems", groundingContext.evidenceItems().stream()
				.map(ProjectRoomGroundingEvidence::toPayload)
				.toList());
		payload.put("retrievalModes", groundingContext.retrievalModes());
		payload.put("ragGrounded", groundingContext.hasDocumentEvidence());
		payload.put("ragMaxSimilarity", groundingContext.ragMaxSimilarity());
		payload.put("ragHits", ragHits(groundingContext));
		payload.put("resourceIds", groundingContext.resourceIds());
		payload.put("matchedResources", matchedEvidence(groundingContext, ProjectRoomGroundingSourceType.DOCUMENT));
		payload.put("citations", citations(groundingContext));
		payload.put("taskIds", groundingContext.taskIds());
		payload.put("matchedTasks", matchedEvidence(groundingContext, ProjectRoomGroundingSourceType.TASK));
		payload.put("wbsItemIds", groundingContext.wbsItemIds());
		payload.put("matchedWbsItems", matchedEvidence(groundingContext, ProjectRoomGroundingSourceType.WBS));
		payload.put("scheduleIds", groundingContext.scheduleIds());
		payload.put("agentSuggestionIds", groundingContext.agentSuggestionIds());
	}

	private String answerCompleteness(String answer, String fallbackReason, ProjectRoomGroundingContext groundingContext) {
		if (!groundingContext.grounded() || fallbackReason != null) {
			return "NO_EVIDENCE";
		}
		String normalized = normalize(answer);
		if (containsAny(normalized, "추가 확인", "확인 필요", "불명확", "부족", "missing", "unclear", "不明", "確認が必要")) {
			return "PARTIAL";
		}
		return "ANSWERED";
	}

	private List<String> missingInfo(
			String request,
			String answer,
			String fallbackReason,
			ProjectRoomGroundingContext groundingContext
	) {
		if (FALLBACK_AMBIGUOUS_RESOURCE_INTENT.equals(fallbackReason)) {
			return List.of("AMBIGUOUS_RESOURCE_INTENT");
		}
		if (groundingContext.retrievalFailed()) {
			if (AgentQuerySupport.isDocumentSourceRequest(request)) {
				return List.of("DOCUMENT_RETRIEVAL_FAILED");
			}
			return List.of("GROUNDING_RETRIEVAL_FAILED");
		}
		if (!groundingContext.grounded()) {
			if (AgentQuerySupport.isDocumentSourceRequest(request)) {
				return List.of("NO_RELEVANT_DOCUMENT");
			}
			return List.of("NO_RELEVANT_PROJECT_GROUNDING");
		}
		String normalized = normalize(answer);
		if (containsAny(normalized, "추가 확인", "확인 필요", "missing", "unclear", "確認が必要")) {
			return List.of("PARTIAL_EVIDENCE");
		}
		return List.of();
	}

	private List<Map<String, Object>> matchedEvidence(
			ProjectRoomGroundingContext groundingContext,
			ProjectRoomGroundingSourceType sourceType
	) {
		return groundingContext.evidenceItems().stream()
				.filter(evidence -> evidence.sourceType() == sourceType)
				.map(evidence -> {
					Map<String, Object> item = new LinkedHashMap<>();
					item.put("id", evidence.id());
					item.putAll(evidence.metadata());
					return item;
				})
				.toList();
	}

	private List<Map<String, Object>> citations(ProjectRoomGroundingContext groundingContext) {
		List<Map<String, Object>> citations = new ArrayList<>();
		for (ProjectRoomGroundingEvidence evidence : groundingContext.evidenceItems()) {
			Object title = citationTitle(evidence);
			if (title == null || title.toString().isBlank()) {
				continue;
			}
			Map<String, Object> citation = new LinkedHashMap<>();
			citation.put("sourceType", evidence.sourceType().name());
			citation.put("sourceId", evidence.id());
			citation.put("resourceId", evidence.sourceType() == ProjectRoomGroundingSourceType.DOCUMENT
					? evidence.id()
					: evidence.metadata().get("resourceId"));
			citation.put("retrievalMode", firstNonNull(
					evidence.metadata().get("retrievalMode"),
					evidence.sourceType() == ProjectRoomGroundingSourceType.DOCUMENT ? null : "MANAGEMENT_CONTEXT"
			));
			citation.put("title", title);
			citation.put("pageNumber", evidence.metadata().get("pageNumber"));
			citation.put("chunkIndex", evidence.metadata().get("chunkIndex"));
			citation.put("startLine", evidence.metadata().get("startLine"));
			citation.put("endLine", evidence.metadata().get("endLine"));
			citation.put("startOffset", evidence.metadata().get("startOffset"));
			citation.put("endOffset", evidence.metadata().get("endOffset"));
			citation.put("quote", evidence.metadata().get("quote"));
			citation.put("similarityScore", evidence.metadata().get("similarityScore"));
			citation.put("fusionScore", evidence.metadata().get("fusionScore"));
			citation.put("rrfScore", evidence.metadata().get("rrfScore"));
			citation.put("answerabilityScore", evidence.metadata().get("answerabilityScore"));
			citation.put("answerabilityReason", evidence.metadata().get("answerabilityReason"));
			citations.add(citation);
		}
		return citations;
	}

	private Object citationTitle(ProjectRoomGroundingEvidence evidence) {
		Object title = firstNonNull(evidence.metadata().get("originalName"), evidence.metadata().get("title"));
		if (title != null && !title.toString().isBlank()) {
			return title;
		}
		if (evidence.sourceType() == ProjectRoomGroundingSourceType.AGENT_SUGGESTION) {
			Object payload = evidence.metadata().get("payload");
			if (payload instanceof Map<?, ?> payloadMap) {
				Object payloadTitle = payloadMap.get("title");
				if (payloadTitle != null && !payloadTitle.toString().isBlank()) {
					return payloadTitle;
				}
			}
		}
		return switch (evidence.sourceType()) {
			case DOCUMENT -> null;
			case TASK -> "TODO";
			case WBS -> "WBS";
			case SCHEDULE -> "일정";
			case AGENT_SUGGESTION -> "AI 후보";
		};
	}

	private Object firstNonNull(Object first, Object second) {
		return first != null ? first : second;
	}

	private List<String> sourceTypes(ProjectRoomGroundingContext groundingContext) {
		return groundingContext.sourceTypes().stream().map(Enum::name).toList();
	}

	private List<Map<String, Object>> ragHits(ProjectRoomGroundingContext groundingContext) {
		return groundingContext.ragHits().stream().map(this::ragHit).toList();
	}

	private Map<String, Object> ragHit(ResourceSearchHit hit) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("resourceId", hit.resourceId());
		payload.put("chunkIndex", hit.chunkIndex());
		payload.put("pageNumber", hit.pageNumber());
		payload.put("startLine", hit.startLine());
		payload.put("endLine", hit.endLine());
		payload.put("startOffset", hit.startOffset());
		payload.put("endOffset", hit.endOffset());
		payload.put("originalName", hit.originalName());
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

	private record AnswerResult(
			String text,
			String fallbackReason
	) {
	}
}
