package com.bubli.agent.dispatch;

import com.bubli.agent.contract.v1.AgentAnalysisResult;
import com.bubli.agent.contract.v1.Suggestion;
import com.bubli.agent.contract.v1.SuggestionType;
import com.bubli.agent.model.AgentAnalysisResultJsonParser;
import com.bubli.agent.model.AiCallExecutor;
import com.bubli.agent.model.AiCallFailedException;
import com.bubli.agent.dto.AgentJobContext;
import com.bubli.agent.service.AgentJobContextCollector;
import com.bubli.agent.service.AgentModelUsageGuard;
import com.bubli.agent.service.AgentModelUsageLimitExceededException;
import com.bubli.agent.type.AgentJobType;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.agent.validation.AgentContractValidationException;
import com.bubli.resource.dto.ResourceAnalysisSource;
import com.bubli.resource.service.ResourceAnalysisPublicService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Profile({"ai", "prod"})
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agent.execution.mode", havingValue = "llm")
public class LlmAgentJobExecutionPort implements AgentJobExecutionPort {

	private static final String PROMPT_VERSION = "agent-job-llm-v1";
	private static final String SCHEMA_VERSION = AgentAnalysisResult.SCHEMA_VERSION;
	private static final String MODEL_NAME = "spring-ai-chat";
	private static final String PROVIDER_ERROR = "AI_PROVIDER_UNAVAILABLE";
	private static final String INVALID_OUTPUT_ERROR = "AI_INVALID_OUTPUT";
	private static final String EXECUTION_ERROR = "AGENT_EXECUTION_FAILED";
	private static final String USAGE_LIMIT_PROMPT = "AI_USAGE_LIMIT_CHECK";
	private static final String EMPTY_RESOURCE_ID = "00000000-0000-0000-0000-000000000000";
	private static final int ANALYSIS_TEXT_LIMIT = 12000;
	private static final int RESPONSE_PREVIEW_LIMIT = 4000;

	private final ResourceAnalysisPublicService resourceAnalysisService;
	private final ChatModel chatModel;
	private final AiCallExecutor aiCallExecutor;
	private final AgentAnalysisResultJsonParser resultJsonParser;
	private final ObjectMapper objectMapper;
	private final AgentJobContextCollector contextCollector;
	private final AgentModelUsageGuard modelUsageGuard;
	private final AgentResourceAnalysisCompletionRecorder resourceAnalysisCompletionRecorder;

	@Override
	public Optional<AgentJobExecutionOutcome> execute(AgentJobQueueMessage message) {
		Instant startedAt = Instant.now();
		try {
			modelUsageGuard.assertWithinDailyLimit(message.requestedByUserId(), message.jobType());
		} catch (AgentModelUsageLimitExceededException exception) {
			return Optional.of(AgentJobExecutionOutcome.failedWithModelCallLogs(
					exception.errorCode(),
					exception.getMessage(),
					List.of(modelCallLog(startedAt, USAGE_LIMIT_PROMPT, null, exception.errorCode()))
			));
		}
		if (message.jobType() == AgentJobType.ANALYZE_RESOURCE) {
			return Optional.of(analyzeResource(message, startedAt));
		}
		AgentJobContext context = contextCollector.collect(message);
		String prompt = promptFor(message, context);
		try {
			ParsedModelResult parsed = callAndParseJson(
					"agent-job-" + message.jobType().name().toLowerCase(),
					prompt
			);
			return Optional.of(AgentJobExecutionOutcome.succeededWithResults(
					toSuggestionDrafts(message, parsed.result()),
					List.of(modelCallLog(startedAt, parsed.prompt(), parsed.response(), null))
			));
		} catch (AgentContractValidationException exception) {
			return Optional.of(AgentJobExecutionOutcome.failedWithModelCallLogs(
					INVALID_OUTPUT_ERROR,
					exception.getMessage(),
					List.of(modelCallLog(startedAt, prompt, null, INVALID_OUTPUT_ERROR))
			));
		} catch (AiCallFailedException exception) {
			return Optional.of(AgentJobExecutionOutcome.failedWithModelCallLogs(
					PROVIDER_ERROR,
					errorMessage(exception),
					List.of(modelCallLog(startedAt, prompt, null, PROVIDER_ERROR))
			));
		} catch (RuntimeException exception) {
			return Optional.of(AgentJobExecutionOutcome.failedWithModelCallLogs(
					EXECUTION_ERROR,
					errorMessage(exception),
					List.of(modelCallLog(startedAt, prompt, null, EXECUTION_ERROR))
			));
		}
	}

	private AgentJobExecutionOutcome analyzeResource(AgentJobQueueMessage message, Instant startedAt) {
		if (message.resourceId() == null) {
			return AgentJobExecutionOutcome.failedWithModelCallLogs(
					"AGENT_RESOURCE_REQUIRED",
					"resourceId is required for ANALYZE_RESOURCE.",
					List.of(modelCallLog(startedAt, "ANALYZE_RESOURCE", null, "AGENT_RESOURCE_REQUIRED"))
			);
		}
		ResourceAnalysisSource source = null;
		String prompt = "ANALYZE_RESOURCE";
		try {
			source = resourceAnalysisService.loadAnalysisSourceForJob(message.resourceId());
			Optional<Map<String, Object>> reusableAnalysis = reusableAnalysis(message);
			if (reusableAnalysis.isPresent()) {
				resourceAnalysisCompletionRecorder.complete(message, source, reusableAnalysis.get(), List.of());
				return AgentJobExecutionOutcome.succeeded();
			}
			AgentJobContext context = contextCollector.collect(message);
			prompt = analyzeResourcePrompt(message, source, context);
			ParsedModelResult parsed = callAndParseJson("agent-job-analyze-resource", prompt);
			AgentAnalysisResult result = parsed.result();
			resourceAnalysisCompletionRecorder.complete(
					message,
					source,
					analysisJson(result, source),
					toSuggestionDrafts(message, result)
			);
			return AgentJobExecutionOutcome.succeededWithModelCallLogs(
					List.of(modelCallLog(startedAt, parsed.prompt(), parsed.response(), null))
			);
		} catch (AgentContractValidationException exception) {
			markAnalysisFailed(source, message);
			return AgentJobExecutionOutcome.failedWithModelCallLogs(
					INVALID_OUTPUT_ERROR,
					exception.getMessage(),
					List.of(modelCallLog(startedAt, prompt, null, INVALID_OUTPUT_ERROR))
			);
		} catch (AiCallFailedException exception) {
			markAnalysisFailed(source, message);
			return AgentJobExecutionOutcome.failedWithModelCallLogs(
					PROVIDER_ERROR,
					errorMessage(exception),
					List.of(modelCallLog(startedAt, prompt, null, PROVIDER_ERROR))
			);
		} catch (RuntimeException exception) {
			markAnalysisFailed(source, message);
			return AgentJobExecutionOutcome.failedWithModelCallLogs(
					EXECUTION_ERROR,
					errorMessage(exception),
					List.of(modelCallLog(startedAt, prompt, null, EXECUTION_ERROR))
			);
		}
	}

	private Optional<Map<String, Object>> reusableAnalysis(AgentJobQueueMessage message) {
		if (!"ko-KR".equals(responseLocale(message))) {
			return Optional.empty();
		}
		return resourceAnalysisService.findReusableAnalysisForJob(message.resourceId());
	}

	private ParsedModelResult callAndParseJson(String operationName, String prompt) {
		String response = aiCallExecutor.execute(operationName, () -> chatModel.call(prompt));
		try {
			return new ParsedModelResult(resultJsonParser.parse(response), response, prompt);
		} catch (AgentContractValidationException firstFailure) {
			String repairPrompt = jsonRepairPrompt(prompt, response, firstFailure);
			String repairedResponse = aiCallExecutor.execute(
					operationName + "-json-repair",
					() -> chatModel.call(repairPrompt)
			);
			try {
				return new ParsedModelResult(resultJsonParser.parse(repairedResponse), repairedResponse, repairPrompt);
			} catch (AgentContractValidationException secondFailure) {
				log.warn(
						"Agent analysis response was not valid JSON after repair. operationName={}, firstResponsePreview={}, repairedResponsePreview={}",
						operationName,
						truncate(response, RESPONSE_PREVIEW_LIMIT),
						truncate(repairedResponse, RESPONSE_PREVIEW_LIMIT),
						secondFailure
				);
				throw secondFailure;
			}
		}
	}

	private String jsonRepairPrompt(String originalPrompt, String invalidResponse, AgentContractValidationException failure) {
		String languageInstruction = languageInstructionFromPrompt(originalPrompt);
		return """
				Your previous answer did not satisfy Bubli's required JSON contract.
				Return ONLY one valid JSON object. Do not include markdown fences, comments, apologies, explanations, or text outside JSON.
				The JSON object must match schemaVersion "%s" and the required shape from the original prompt.
				%s
				If a value is unknown, use a conservative empty array or a concise placeholder in the requested response language, but keep the schema valid.

				Validation error:
				%s

				Previous invalid answer:
				%s

				Original prompt:
				%s
				""".formatted(
				SCHEMA_VERSION,
				languageInstruction,
				failure.getMessage(),
				truncate(invalidResponse, RESPONSE_PREVIEW_LIMIT),
				originalPrompt
		);
	}

	private String analyzeResourcePrompt(
			AgentJobQueueMessage message,
			ResourceAnalysisSource source,
			AgentJobContext context
	) {
		String languageInstruction = languageInstruction(message);
		return """
				You are Bubli's AI document analyzer. Return only valid JSON matching schemaVersion "%s".
				JSON_OUTPUT_ONLY: The first character of your response must be { and the last character must be }.
				Do not include any text before or after the JSON object.
				Do not include markdown fences or explanatory text.
				%s
				Keep sourceText and direct evidence quotes in the original document language.
				Analyze the provided document text for project use. Do not make legal judgments; create review aids only.
				Every suggestion must be grounded in the document text.

				Required JSON shape:
				{
				  "schemaVersion": "%s",
				  "resourceId": "%s",
				  "model": {"name": "spring-ai-chat", "promptVersion": "%s"},
				  "analysis": {
				    "summary": "document summary in the requested response language",
				    "keywords": ["keyword"],
				    "risks": ["review risk or ambiguity"],
				    "checklist": [{"title": "actionable review checklist item", "severity": "MEDIUM"}]
				  },
				  "suggestions": [
				    {
				      "type": "REVIEW_ITEM",
				      "title": "specific title in the requested response language",
				      "description": "specific review action in the requested response language",
				      "sourceText": "short evidence text from the document",
				      "confidence": 0.0,
				      "startsAt": "optional ISO-8601 UTC instant when the document gives a schedule start",
				      "dueAt": "optional ISO-8601 UTC instant when the document gives a due date",
				      "endsAt": "optional ISO-8601 UTC instant when the document gives a schedule end"
				    },
				    {
				      "type": "CONTRACT_FIELD",
				      "title": "field extraction title",
				      "description": "field meaning",
				      "sourceText": "short evidence text from the document",
				      "confidence": 0.0,
				      "fieldKey": "contract_amount",
				      "value": "1500000"
				    }
				  ]
				}

				Suggestion guidance:
				- Use REVIEW_ITEM for review actions.
				- Use QUESTION for missing or ambiguous information.
				- Use CONTRACT_FIELD only when a concrete field value exists; fieldKey and value are required.
				- For CONTRACT_FIELD, prefer these standard fieldKey values when the field matches: contract_amount (total contract or estimate amount, digits only, no currency symbol or thousands separators), payment_due_date (expected payment date as YYYY-MM-DD), payment_date (actual paid date as YYYY-MM-DD), client_name (client or company name), project_name, contract_period, deliverable. Use a concise lowercase snake_case key for any other field.
				- Use REQUIREMENT, TASK, or WBS only when the document clearly implies them.
				- For TASK, you may include assigneeUserId, wbsItemId, status(TODO/IN_PROGRESS/REVIEW/DONE/BLOCKED), dueAt.
				- For WBS, you may include parentId, orderNo, status(TODO/IN_PROGRESS/DONE), scheduleTitle, startsAt, dueAt, endsAt, allDay.
				- Use ISO-8601 UTC instants for startsAt, dueAt, and endsAt. Omit date fields when the source has no concrete date/time.
				- Return 1 to 5 high-value suggestions.

				Job context:
				jobId: %s
				jobType: %s
				requestedByUserId: %s
				roomId: %s
				resourceId: %s
				originalName: %s
				mimeType: %s
				documentType: %s
				pageCount: %s
				characterCount: %s
				requestPayload: %s
				contextCharacters: %s

				Additional project context:
				%s

				Document text:
				%s
				""".formatted(
				SCHEMA_VERSION,
				languageInstruction,
				SCHEMA_VERSION,
				source.resourceId(),
				PROMPT_VERSION,
				message.jobId(),
				message.jobType(),
				message.requestedByUserId(),
				message.roomId(),
				source.resourceId(),
				source.originalName(),
				source.mimeType(),
				source.documentType(),
				source.pageCount(),
				source.characterCount(),
				message.requestPayload(),
				context.characterCount(),
				context.promptBlock(),
				truncate(source.text(), ANALYSIS_TEXT_LIMIT)
		);
	}

	private Map<String, Object> analysisJson(AgentAnalysisResult result, ResourceAnalysisSource source) {
		Map<String, Object> analysis = new LinkedHashMap<>(
				objectMapper.convertValue(result.analysis(), new TypeReference<Map<String, Object>>() {
				})
		);
		analysis.put("schemaVersion", result.schemaVersion());
		analysis.put("model", objectMapper.convertValue(result.model(), new TypeReference<Map<String, Object>>() {
		}));
		analysis.put("documentType", source.documentType().name());
		analysis.put("suggestionCount", result.suggestions().size());
		return analysis;
	}

	private void markAnalysisFailed(ResourceAnalysisSource source, AgentJobQueueMessage message) {
		if (source != null) {
			resourceAnalysisService.markAnalysisFailed(source.resourceId());
			return;
		}
		if (message.resourceId() != null) {
			resourceAnalysisService.markAnalysisFailed(message.resourceId());
		}
	}

	private String promptFor(AgentJobQueueMessage message, AgentJobContext context) {
		String languageInstruction = languageInstruction(message);
		return """
				You are Bubli's project assistant. Return only valid JSON matching schemaVersion "%s".
				JSON_OUTPUT_ONLY: The first character of your response must be { and the last character must be }.
				Do not include any text before or after the JSON object.
				Do not include markdown fences or explanatory text.
				%s
				Keep sourceText and direct evidence quotes in the original source language.
				Do not copy placeholder phrases from this prompt.
				Make the suggestion specific to the jobType and requestPayload.
				If detailed project context is missing, create a conservative, useful first draft instead of a generic label.

				Required JSON shape:
				{
				  "schemaVersion": "%s",
				  "resourceId": "%s",
				  "model": {"name": "spring-ai-chat", "promptVersion": "%s"},
				  "analysis": {"summary": "concise summary in the requested response language", "keywords": [], "risks": [], "checklist": []},
				  "suggestions": [
				    {
				      "type": "%s",
				      "title": "specific actionable title in the requested response language",
				      "description": "specific description with expected next action in the requested response language",
				      "sourceText": "brief reason based on the job context",
				      "confidence": 0.0,
				      "startsAt": "optional ISO-8601 UTC instant for WBS or schedule-like work",
				      "dueAt": "optional ISO-8601 UTC due date/time for TASK or WBS",
				      "endsAt": "optional ISO-8601 UTC end time for WBS schedule",
				      "documentType": "required for DOCUMENT_DRAFT. One of PROJECT_BRIEF, CLARIFICATION_ITEMS, CLIENT_QUESTIONS, MEETING_NOTE, WBS_TODO_PLAN",
				      "contentMarkdown": "required only for DOCUMENT_DRAFT. Full Markdown document body, not a one-line summary."
				    }
				  ]
				}

				Job type guidance:
				- GENERATE_REQUIREMENTS: propose one concrete requirement.
				- GENERATE_TASKS: propose one actionable TODO task with a clear verb.
				- GENERATE_WBS: propose one WBS work item. Include scheduleTitle, startsAt/dueAt, endsAt, and allDay only when the request or context has a concrete date/time.
				- GENERATE_QUESTIONS: propose one clarification question.
				- REVIEW_CONTRACT_DOCUMENTS: propose one document review item.
				- DRAFT_DOCUMENT: write a complete freelancer-facing document draft in contentMarkdown. Include a title, overview, WBS section, TODO section, assumptions, and source/evidence notes when available. Set documentType to PROJECT_BRIEF, CLARIFICATION_ITEMS, CLIENT_QUESTIONS, MEETING_NOTE, or WBS_TODO_PLAN. Do not put the real document body only in description.
				- DAILY_SUMMARY: propose a daily summary draft using only the target date context. The description must contain JSON-like fields: summaryDate, timezone, done, remaining, todaySchedules, tomorrowFocus, risks, evidence. Do not include private raw source text beyond the provided concise evidence labels.

				Job context:
				jobId: %s
				jobType: %s
				requestedByUserId: %s
				roomId: %s
				resourceId: %s
				requestPayload: %s
				contextCharacters: %s

				Additional project context:
				%s
				""".formatted(
				SCHEMA_VERSION,
				languageInstruction,
				SCHEMA_VERSION,
				message.resourceId() == null ? EMPTY_RESOURCE_ID : message.resourceId(),
				PROMPT_VERSION,
				suggestionType(message.jobType()).name(),
				message.jobId(),
				message.jobType(),
				message.requestedByUserId(),
				message.roomId(),
				message.resourceId(),
				message.requestPayload(),
				context.characterCount(),
				context.promptBlock()
		);
	}

	private List<AgentJobExecutionSuggestionDraft> toSuggestionDrafts(
			AgentJobQueueMessage message,
			AgentAnalysisResult result
	) {
		return result.suggestions().stream()
				.map(suggestion -> new AgentJobExecutionSuggestionDraft(
						toAgentSuggestionType(suggestion.type()),
						json(payload(message, suggestion)),
						json(evidence(message, result, suggestion))
				))
				.toList();
	}

	private Map<String, Object> payload(AgentJobQueueMessage message, Suggestion suggestion) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("title", suggestion.title());
		payload.put("description", suggestion.description());
		payload.put("sourceText", suggestion.sourceText());
		payload.put("confidence", suggestion.confidence());
		payload.put("fieldKey", suggestion.fieldKey());
		payload.put("value", suggestion.value());
		putIfPresent(payload, "assigneeUserId", suggestion.assigneeUserId());
		putIfPresent(payload, "wbsItemId", suggestion.wbsItemId());
		putIfPresent(payload, "parentId", suggestion.parentId());
		putIfPresent(payload, "orderNo", suggestion.orderNo());
		putIfPresent(payload, "status", suggestion.status());
		putIfPresent(payload, "startsAt", suggestion.startsAt());
		putIfPresent(payload, "dueAt", suggestion.dueAt());
		putIfPresent(payload, "endsAt", suggestion.endsAt());
		putIfPresent(payload, "allDay", suggestion.allDay());
		putIfPresent(payload, "scheduleTitle", suggestion.scheduleTitle());
		putIfPresent(payload, "documentType", suggestion.documentType());
		putIfPresent(payload, "contentMarkdown", suggestion.contentMarkdown());
		payload.put("jobType", message.jobType().name());
		payload.put("roomId", value(message.roomId()));
		payload.put("resourceId", value(message.resourceId()));
		payload.put("source", "LLM");
		if (message.requestPayload() != null) {
			payload.putAll(message.requestPayload());
		}
		return payload;
	}

	private Map<String, Object> evidence(
			AgentJobQueueMessage message,
			AgentAnalysisResult result,
			Suggestion suggestion
	) {
		Map<String, Object> evidence = new LinkedHashMap<>();
		evidence.put("jobId", value(message.jobId()));
		evidence.put("requestedByUserId", value(message.requestedByUserId()));
		evidence.put("promptVersion", result.model().promptVersion());
		evidence.put("schemaVersion", result.schemaVersion());
		evidence.put("modelName", result.model().name());
		evidence.put("sourceText", suggestion.sourceText());
		evidence.put("analysisSummary", result.analysis().summary());
		return evidence;
	}

	private SuggestionType suggestionType(AgentJobType jobType) {
		return switch (jobType) {
			case GENERATE_REQUIREMENTS -> SuggestionType.REQUIREMENT;
			case GENERATE_TASKS -> SuggestionType.TASK;
			case GENERATE_WBS -> SuggestionType.WBS;
			case GENERATE_QUESTIONS -> SuggestionType.QUESTION;
			case REVIEW_CONTRACT_DOCUMENTS -> SuggestionType.REVIEW_ITEM;
			case DRAFT_DOCUMENT -> SuggestionType.DOCUMENT_DRAFT;
			case DAILY_SUMMARY -> SuggestionType.DAILY_SUMMARY;
			case ANALYZE_RESOURCE -> SuggestionType.REVIEW_ITEM;
		};
	}

	private AgentSuggestionType toAgentSuggestionType(SuggestionType suggestionType) {
		return switch (suggestionType) {
			case TASK -> AgentSuggestionType.TASK;
			case REQUIREMENT -> AgentSuggestionType.REQUIREMENT;
			case WBS -> AgentSuggestionType.WBS;
			case QUESTION -> AgentSuggestionType.QUESTION;
			case CONTRACT_FIELD -> AgentSuggestionType.CONTRACT_FIELD;
			case REVIEW_ITEM -> AgentSuggestionType.REVIEW_ITEM;
			case DOCUMENT_DRAFT -> AgentSuggestionType.DOCUMENT_DRAFT;
			case DAILY_SUMMARY -> AgentSuggestionType.DAILY_SUMMARY;
		};
	}

	private AgentJobExecutionModelCallLog modelCallLog(
			Instant startedAt,
			String prompt,
			String response,
			String errorCode
	) {
		return new AgentJobExecutionModelCallLog(
				PROMPT_VERSION,
				SCHEMA_VERSION,
				MODEL_NAME,
				Duration.between(startedAt, Instant.now()).toMillis(),
				estimateTokens(prompt),
				estimateTokens(response),
				errorCode
		);
	}

	private Integer estimateTokens(String text) {
		if (text == null || text.isBlank()) {
			return 0;
		}
		return Math.max(1, text.length() / 4);
	}

	private String json(Map<String, Object> value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize agent execution payload.", exception);
		}
	}

	private String value(Object value) {
		return value == null ? null : value.toString();
	}

	private void putIfPresent(Map<String, Object> payload, String key, Object value) {
		if (value != null) {
			payload.put(key, value);
		}
	}

	private String truncate(String text, int limit) {
		if (text == null || text.length() <= limit) {
			return text;
		}
		return text.substring(0, limit);
	}

	private String languageInstruction(AgentJobQueueMessage message) {
		return switch (responseLocale(message)) {
			case "en-US" -> "Write all user-facing content in natural English.";
			case "ja-JP" -> "Write all user-facing content in natural Japanese.";
			default -> "Write all user-facing content in natural Korean.";
		};
	}

	private String responseLocale(AgentJobQueueMessage message) {
		String locale = requestPayloadText(message, "locale");
		if (locale == null || locale.isBlank()) {
			return "ko-KR";
		}
		String normalized = locale.trim();
		if ("en-US".equalsIgnoreCase(normalized) || "en".equalsIgnoreCase(normalized)) {
			return "en-US";
		}
		if ("ja-JP".equalsIgnoreCase(normalized) || "ja".equalsIgnoreCase(normalized)) {
			return "ja-JP";
		}
		return "ko-KR";
	}

	private String languageInstructionFromPrompt(String prompt) {
		if (prompt != null && prompt.contains("natural English")) {
			return "Write all user-facing content in natural English.";
		}
		if (prompt != null && prompt.contains("natural Japanese")) {
			return "Write all user-facing content in natural Japanese.";
		}
		return "Write all user-facing content in natural Korean.";
	}

	private String requestPayloadText(AgentJobQueueMessage message, String key) {
		if (message.requestPayload() == null) {
			return null;
		}
		Object value = message.requestPayload().get(key);
		return value == null ? null : value.toString();
	}

	private String errorMessage(RuntimeException exception) {
		String message = exception.getMessage();
		return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
	}

	private record ParsedModelResult(
			AgentAnalysisResult result,
			String response,
			String prompt
	) {
	}
}
