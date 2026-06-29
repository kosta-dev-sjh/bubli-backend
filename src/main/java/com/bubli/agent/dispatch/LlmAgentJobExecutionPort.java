package com.bubli.agent.dispatch;

import com.bubli.agent.contract.v1.AgentAnalysisResult;
import com.bubli.agent.contract.v1.Suggestion;
import com.bubli.agent.contract.v1.SuggestionType;
import com.bubli.agent.model.AgentAnalysisResultJsonParser;
import com.bubli.agent.model.AiCallExecutor;
import com.bubli.agent.model.AiCallFailedException;
import com.bubli.agent.type.AgentJobType;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.agent.validation.AgentContractValidationException;
import com.bubli.resource.service.ResourceAnalysisPublicService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
@Profile("ai")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agent.execution.mode", havingValue = "llm")
public class LlmAgentJobExecutionPort implements AgentJobExecutionPort {

	private static final String PROMPT_VERSION = "agent-job-llm-v1";
	private static final String SCHEMA_VERSION = AgentAnalysisResult.SCHEMA_VERSION;
	private static final String MODEL_NAME = "spring-ai-chat";
	private static final String PROVIDER_ERROR = "AI_PROVIDER_UNAVAILABLE";
	private static final String INVALID_OUTPUT_ERROR = "AI_INVALID_OUTPUT";
	private static final String EXECUTION_ERROR = "AGENT_EXECUTION_FAILED";
	private static final String EMPTY_RESOURCE_ID = "00000000-0000-0000-0000-000000000000";

	private final ResourceAnalysisPublicService resourceAnalysisService;
	private final ChatModel chatModel;
	private final AiCallExecutor aiCallExecutor;
	private final AgentAnalysisResultJsonParser resultJsonParser;
	private final ObjectMapper objectMapper;

	@Override
	public Optional<AgentJobExecutionOutcome> execute(AgentJobQueueMessage message) {
		Instant startedAt = Instant.now();
		if (message.jobType() == AgentJobType.ANALYZE_RESOURCE) {
			return Optional.of(analyzeResource(message, startedAt));
		}
		String prompt = promptFor(message);
		try {
			String response = aiCallExecutor.execute(
					"agent-job-" + message.jobType().name().toLowerCase(),
					() -> chatModel.call(prompt)
			);
			AgentAnalysisResult result = resultJsonParser.parse(response);
			return Optional.of(AgentJobExecutionOutcome.succeededWithResults(
					toSuggestionDrafts(message, result),
					List.of(modelCallLog(startedAt, prompt, response, null))
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
		try {
			resourceAnalysisService.analyzeResourceForJob(message.resourceId(), message.jobId());
			return AgentJobExecutionOutcome.succeededWithModelCallLogs(
					List.of(modelCallLog(startedAt, "ANALYZE_RESOURCE", "", null))
			);
		} catch (RuntimeException exception) {
			return AgentJobExecutionOutcome.failedWithModelCallLogs(
					EXECUTION_ERROR,
					errorMessage(exception),
					List.of(modelCallLog(startedAt, "ANALYZE_RESOURCE", null, EXECUTION_ERROR))
			);
		}
	}

	private String promptFor(AgentJobQueueMessage message) {
		return """
				You are Bubli's project assistant. Return only valid JSON matching schemaVersion "%s".
				Do not include markdown fences or explanatory text.

				Required JSON shape:
				{
				  "schemaVersion": "%s",
				  "resourceId": "%s",
				  "model": {"name": "spring-ai-chat", "promptVersion": "%s"},
				  "analysis": {"summary": "short Korean summary", "keywords": [], "risks": [], "checklist": []},
				  "suggestions": [
				    {
				      "type": "%s",
				      "title": "short Korean title",
				      "description": "Korean description",
				      "sourceText": "brief evidence or reason",
				      "confidence": 0.0
				    }
				  ]
				}

				Job context:
				jobId: %s
				jobType: %s
				requestedByUserId: %s
				roomId: %s
				resourceId: %s
				requestPayload: %s
				""".formatted(
				SCHEMA_VERSION,
				SCHEMA_VERSION,
				message.resourceId() == null ? EMPTY_RESOURCE_ID : message.resourceId(),
				PROMPT_VERSION,
				suggestionType(message.jobType()).name(),
				message.jobId(),
				message.jobType(),
				message.requestedByUserId(),
				message.roomId(),
				message.resourceId(),
				message.requestPayload()
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

	private String errorMessage(RuntimeException exception) {
		String message = exception.getMessage();
		return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
	}
}
