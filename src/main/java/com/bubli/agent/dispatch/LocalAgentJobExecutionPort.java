package com.bubli.agent.dispatch;

import com.bubli.agent.type.AgentJobType;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.resource.service.ResourceAnalysisPublicService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agent.execution.mode", havingValue = "local", matchIfMissing = true)
public class LocalAgentJobExecutionPort implements AgentJobExecutionPort {

    private static final String PROMPT_VERSION = "local-step-6.4";
    private static final String SCHEMA_VERSION = "local-v1";
    private static final String MODEL_NAME = "local-deterministic";

    private final ResourceAnalysisPublicService resourceAnalysisService;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<AgentJobExecutionOutcome> execute(AgentJobQueueMessage message) {
        try {
            if (message.jobType() == AgentJobType.ANALYZE_RESOURCE) {
                return Optional.of(analyzeResource(message));
            }
            return Optional.of(generateSuggestion(message));
        } catch (RuntimeException exception) {
            return Optional.of(AgentJobExecutionOutcome.failed(
                    "AGENT_EXECUTION_FAILED",
                    errorMessage(exception)
            ));
        }
    }

    private AgentJobExecutionOutcome analyzeResource(AgentJobQueueMessage message) {
        if (message.resourceId() == null) {
            return AgentJobExecutionOutcome.failed(
                    "AGENT_RESOURCE_REQUIRED",
                    "resourceId is required for ANALYZE_RESOURCE."
            );
        }
        resourceAnalysisService.analyzeResourceForJob(message.resourceId(), message.jobId());
        return AgentJobExecutionOutcome.succeededWithModelCallLogs(modelCallLog(null));
    }

    private AgentJobExecutionOutcome generateSuggestion(AgentJobQueueMessage message) {
        String locale = locale(message);
        AgentSuggestionType suggestionType = suggestionType(message.jobType());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title(message.jobType(), locale));
        payload.put("description", description(message.jobType(), locale));
        payload.put("jobType", message.jobType().name());
        payload.put("roomId", value(message.roomId()));
        payload.put("resourceId", value(message.resourceId()));
        payload.put("source", MODEL_NAME);
        payload.putAll(enrichPayload(message, locale));

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("jobId", value(message.jobId()));
        evidence.put("requestedByUserId", value(message.requestedByUserId()));
        evidence.put("promptVersion", PROMPT_VERSION);
        evidence.put("schemaVersion", SCHEMA_VERSION);
        evidence.put("modelName", MODEL_NAME);

        return AgentJobExecutionOutcome.succeededWithResults(
                List.of(new AgentJobExecutionSuggestionDraft(
                        suggestionType,
                        json(payload),
                        json(evidence)
                )),
                modelCallLog(null)
        );
    }

    private AgentSuggestionType suggestionType(AgentJobType jobType) {
        return switch (jobType) {
            case GENERATE_REQUIREMENTS -> AgentSuggestionType.REQUIREMENT;
            case GENERATE_TASKS -> AgentSuggestionType.TASK;
            case GENERATE_WBS -> AgentSuggestionType.WBS;
            case GENERATE_QUESTIONS -> AgentSuggestionType.QUESTION;
            case REVIEW_CONTRACT_DOCUMENTS -> AgentSuggestionType.REVIEW_ITEM;
            case DRAFT_DOCUMENT -> AgentSuggestionType.DOCUMENT_DRAFT;
            case DAILY_SUMMARY -> AgentSuggestionType.DAILY_SUMMARY;
            case ANALYZE_RESOURCE -> AgentSuggestionType.REVIEW_ITEM;
        };
    }

    private String title(AgentJobType jobType, String locale) {
        return switch (locale) {
            case "en-US" -> englishTitle(jobType);
            case "ja-JP" -> japaneseTitle(jobType);
            default -> koreanTitle(jobType);
        };
    }

    private String description(AgentJobType jobType, String locale) {
        return switch (locale) {
            case "en-US" -> "Created a %s.".formatted(englishTitle(jobType).toLowerCase());
            case "ja-JP" -> "%sを作成しました。".formatted(japaneseTitle(jobType));
            default -> "%s를 생성했습니다.".formatted(koreanTitle(jobType));
        };
    }

    private Map<String, Object> enrichPayload(AgentJobQueueMessage message, String locale) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> requestPayload = message.requestPayload() == null ? Map.of() : message.requestPayload();
        if (message.jobType() == AgentJobType.DAILY_SUMMARY) {
            String summaryDate = text(requestPayload.get("summaryDate"));
            if (summaryDate == null) {
                summaryDate = LocalDate.now().toString();
            }
            String timezone = defaultText(requestPayload.get("timezone"), "Asia/Seoul");
            payload.put("summaryDate", summaryDate);
            payload.put("summaryJson", json(Map.of(
                    "summaryDate", summaryDate,
                    "timezone", timezone,
                    "done", List.of(),
                    "remaining", List.of(),
                    "todaySchedules", List.of(),
                    "tomorrowFocus", List.of(),
                    "risks", List.of(),
                    "evidence", List.of(localEvidence(locale))
            )));
            return payload;
        }
        if (message.jobType() == AgentJobType.DRAFT_DOCUMENT) {
            payload.put("documentType", defaultText(requestPayload.get("documentType"), "GENERAL"));
            payload.put("instruction", defaultText(requestPayload.get("instruction"), ""));
            payload.put("sourceResourceIds", requestPayload.getOrDefault("sourceResourceIds", List.of()));
            payload.put("contentMarkdown", draftContent(locale));
        }
        return payload;
    }

    private String koreanTitle(AgentJobType jobType) {
        return switch (jobType) {
            case GENERATE_REQUIREMENTS -> "요구사항 후보";
            case GENERATE_TASKS -> "작업 후보";
            case GENERATE_WBS -> "WBS 후보";
            case GENERATE_QUESTIONS -> "확인 질문 후보";
            case REVIEW_CONTRACT_DOCUMENTS -> "문서 검토 항목 후보";
            case DRAFT_DOCUMENT -> "문서 초안 후보";
            case DAILY_SUMMARY -> "일일 요약 후보";
            case ANALYZE_RESOURCE -> "자료 분석 결과";
        };
    }

    private String englishTitle(AgentJobType jobType) {
        return switch (jobType) {
            case GENERATE_REQUIREMENTS -> "Requirement candidate";
            case GENERATE_TASKS -> "Task candidate";
            case GENERATE_WBS -> "WBS candidate";
            case GENERATE_QUESTIONS -> "Clarification question candidate";
            case REVIEW_CONTRACT_DOCUMENTS -> "Document review item candidate";
            case DRAFT_DOCUMENT -> "Document draft candidate";
            case DAILY_SUMMARY -> "Daily summary candidate";
            case ANALYZE_RESOURCE -> "Resource analysis result";
        };
    }

    private String japaneseTitle(AgentJobType jobType) {
        return switch (jobType) {
            case GENERATE_REQUIREMENTS -> "要件候補";
            case GENERATE_TASKS -> "タスク候補";
            case GENERATE_WBS -> "WBS候補";
            case GENERATE_QUESTIONS -> "確認質問候補";
            case REVIEW_CONTRACT_DOCUMENTS -> "文書レビュー項目候補";
            case DRAFT_DOCUMENT -> "文書ドラフト候補";
            case DAILY_SUMMARY -> "日次要約候補";
            case ANALYZE_RESOURCE -> "資料分析結果";
        };
    }

    private String localEvidence(String locale) {
        return switch (locale) {
            case "en-US" -> "Local deterministic daily summary.";
            case "ja-JP" -> "ローカルの決定的な日次要約です。";
            default -> "로컬 결정형 일일 요약입니다.";
        };
    }

    private String draftContent(String locale) {
        return switch (locale) {
            case "en-US" -> "# Draft Document\n\nLocal deterministic draft.";
            case "ja-JP" -> "# 文書ドラフト\n\nローカルの決定的なドラフトです。";
            default -> "# 문서 초안\n\n로컬 결정형 초안입니다.";
        };
    }

    private List<AgentJobExecutionModelCallLog> modelCallLog(String errorCode) {
        return List.of(new AgentJobExecutionModelCallLog(
                PROMPT_VERSION,
                SCHEMA_VERSION,
                MODEL_NAME,
                0L,
                0,
                0,
                errorCode
        ));
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

    private String text(Object value) {
        String text = value(value);
        return text == null || text.isBlank() ? null : text.trim();
    }

    private String defaultText(Object value, String defaultValue) {
        String text = text(value);
        return text == null ? defaultValue : text;
    }

    private String locale(AgentJobQueueMessage message) {
        return defaultText(message.requestPayload() == null ? null : message.requestPayload().get("locale"), "ko-KR");
    }

    private String errorMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
