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

    private static final String PROMPT_VERSION = "local-step-6.5";
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
            markResourceAnalysisFailed(message, exception);
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

    private void markResourceAnalysisFailed(AgentJobQueueMessage message, RuntimeException originalException) {
        if (message.jobType() != AgentJobType.ANALYZE_RESOURCE || message.resourceId() == null) {
            return;
        }
        try {
            resourceAnalysisService.markAnalysisFailed(message.resourceId());
        } catch (RuntimeException failure) {
            originalException.addSuppressed(failure);
        }
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
            String documentType = documentDraftType(requestPayload);
            String instruction = defaultText(requestPayload.get("instruction"), "");
            payload.put("documentType", documentType);
            payload.put("instruction", instruction);
            payload.put("sourceResourceIds", requestPayload.getOrDefault("sourceResourceIds", List.of()));
            payload.put("contentMarkdown", draftContent(documentType, instruction, locale));
        }
        if (message.jobType() == AgentJobType.GENERATE_TASKS) {
            copyIfPresent(payload, requestPayload, "assigneeUserId");
            copyIfPresent(payload, requestPayload, "wbsItemId");
            copyIfPresent(payload, requestPayload, "status");
            copyIfPresent(payload, requestPayload, "dueAt");
        }
        if (message.jobType() == AgentJobType.GENERATE_WBS) {
            copyIfPresent(payload, requestPayload, "parentId");
            copyIfPresent(payload, requestPayload, "orderNo");
            copyIfPresent(payload, requestPayload, "status");
            copyIfPresent(payload, requestPayload, "startsAt");
            copyIfPresent(payload, requestPayload, "dueAt");
            copyIfPresent(payload, requestPayload, "endsAt");
            copyIfPresent(payload, requestPayload, "allDay");
            copyIfPresent(payload, requestPayload, "scheduleTitle");
        }
        return payload;
    }

    private void copyIfPresent(Map<String, Object> payload, Map<String, Object> requestPayload, String key) {
        if (requestPayload.containsKey(key) && requestPayload.get(key) != null) {
            payload.put(key, requestPayload.get(key));
        }
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
            case DAILY_SUMMARY -> "日次サマリー候補";
            case ANALYZE_RESOURCE -> "資料分析結果";
        };
    }

    private String localEvidence(String locale) {
        return switch (locale) {
            case "en-US" -> "Local deterministic daily summary.";
            case "ja-JP" -> "ローカル決定論による日次サマリーです。";
            default -> "로컬 결정론 기반 일일 요약입니다.";
        };
    }

    private String documentDraftType(Map<String, Object> requestPayload) {
        String requestedType = defaultText(requestPayload.get("documentType"), "PROJECT_BRIEF");
        String normalized = requestedType.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "PROJECT_BASELINE", "PROJECT_BRIEF", "PROJECT_OVERVIEW", "PROJECT_SUMMARY", "BASELINE" ->
                    "PROJECT_BRIEF";
            case "CHECK_ITEMS", "CLARIFICATION_ITEMS", "CONFIRMATION_ITEMS", "REVIEW_ITEMS", "ISSUE_REVIEW" ->
                    "CLARIFICATION_ITEMS";
            case "CLIENT_QUESTIONS", "QUESTION_DRAFT", "QUESTIONS", "CLIENT_MESSAGE" -> "CLIENT_QUESTIONS";
            case "MEETING_NOTE", "MEETING_SUMMARY", "MINUTES" -> "MEETING_NOTE";
            case "WBS_TODO_PLAN", "WBS_TODO", "EXECUTION_PLAN", "WORK_PLAN", "WBS" -> "WBS_TODO_PLAN";
            default -> requestedType;
        };
    }

    private String draftContent(String documentType, String instruction, String locale) {
        if ("en-US".equals(locale)) {
            return englishDraftContent(documentType, instruction);
        }
        if ("ja-JP".equals(locale)) {
            return japaneseDraftContent(documentType, instruction);
        }
        return koreanDraftContent(documentType, instruction);
    }

    private String koreanDraftContent(String documentType, String instruction) {
        String note = instruction.isBlank() ? "자료를 추가로 확인해 빈 항목을 채워주세요." : instruction;
        return switch (documentType) {
            case "CLARIFICATION_ITEMS" -> """
                    # 확인 필요 항목 정리서

                    ## 문서 간 차이
                    - 확인 필요: 계약서, 요구사항, 회의록의 일정과 범위를 대조해야 합니다.

                    ## 빠진 정보
                    - 클라이언트명:
                    - 최종 납품일:
                    - 납품물 기준:

                    ## 클라이언트에게 확인할 말
                    - 문서마다 기준이 다르게 보이는 항목의 최종 기준을 확인 부탁드립니다.

                    ## 근거 자료
                    - %s
                    """.formatted(note);
            case "CLIENT_QUESTIONS" -> """
                    # 클라이언트 질문 초안

                    안녕하세요. 프로젝트 진행 기준을 정확히 맞추기 위해 아래 항목 확인 부탁드립니다.

                    1. 최종 납품일은 어느 날짜를 기준으로 보면 될까요?
                    2. 이번 범위에 포함되는 납품물과 제외되는 항목을 확인 부탁드립니다.
                    3. 우선순위가 높은 기능이나 화면이 있다면 알려주세요.

                    감사합니다.

                    ## 작성 기준
                    - %s
                    """.formatted(note);
            case "MEETING_NOTE" -> """
                    # 회의록 정리 초안

                    ## 결정사항
                    - 회의에서 확정된 범위와 일정은 자료 대조 후 기입합니다.

                    ## 할 일
                    - 요구사항 변경 여부 확인
                    - 납품물 기준 정리
                    - 다음 확인 질문 작성

                    ## 바뀐 요구사항
                    - 변경 전:
                    - 변경 후:

                    ## 다음 질문
                    - 일정, 범위, 검수 기준 중 아직 모호한 항목을 확인합니다.

                    ## 근거 자료
                    - %s
                    """.formatted(note);
            case "WBS_TODO_PLAN" -> """
                    # WBS/TODO 실행계획 초안

                    ## 큰 작업
                    | 작업 | 세부 작업 | 담당자 | 마감 | 근거 자료 |
                    | --- | --- | --- | --- | --- |
                    | 기준 정리 | 계약서/요구사항/회의록 대조 |  |  | %s |
                    | 요구사항 확정 | 기능, 조건, 제외 범위 정리 |  |  |  |
                    | 납품 준비 | 산출물 검수 기준 확인 |  |  |  |

                    ## 승인 후 전환 후보
                    - WBS 후보
                    - TODO 후보
                    - 일정 후보
                    """.formatted(note);
            default -> """
                    # 프로젝트 기준 정리서

                    ## 프로젝트 목적
                    - 클라이언트 자료를 기준으로 프로젝트가 해결하려는 목표를 정리합니다.

                    ## 작업 범위
                    - 포함 범위:
                    - 제외 범위:

                    ## 납품물
                    - 최종 산출물:
                    - 중간 공유물:

                    ## 일정
                    - 시작일:
                    - 중간 확인:
                    - 최종 납품일:

                    ## 클라이언트 및 참고 자료
                    - 클라이언트명:
                    - 참고 자료: %s

                    ## 확인 필요 항목
                    - 문서마다 다르게 적힌 일정, 범위, 검수 기준을 확인합니다.
                    """.formatted(note);
        };
    }

    private String englishDraftContent(String documentType, String instruction) {
        String note = instruction.isBlank() ? "Fill the empty fields after reviewing the source materials." : instruction;
        return switch (documentType) {
            case "CLARIFICATION_ITEMS" -> "# Clarification Items\n\n## Conflicts\n- Check delivery date, scope, and acceptance criteria across the source documents.\n\n## Missing Information\n- Client name:\n- Final due date:\n- Deliverables:\n\n## Client Check\n- Please confirm the final standard for items that differ between documents.\n\n## Evidence\n- %s".formatted(note);
            case "CLIENT_QUESTIONS" -> "# Client Question Draft\n\nHello,\n\nTo align the project criteria, could you please confirm the items below?\n\n1. Which date should we use as the final delivery date?\n2. Which deliverables are included or excluded from this scope?\n3. Are there high-priority features or screens we should handle first?\n\nThank you.\n\n## Basis\n- %s".formatted(note);
            case "MEETING_NOTE" -> "# Meeting Note Draft\n\n## Decisions\n- Add confirmed scope and dates after comparing the materials.\n\n## Action Items\n- Check requirement changes\n- Summarize deliverable criteria\n- Prepare next questions\n\n## Changed Requirements\n- Before:\n- After:\n\n## Next Questions\n- Confirm unclear schedule, scope, and acceptance items.\n\n## Evidence\n- %s".formatted(note);
            case "WBS_TODO_PLAN" -> "# WBS/TODO Execution Plan Draft\n\n## Work Breakdown\n| Work | Detail | Owner | Due | Evidence |\n| --- | --- | --- | --- | --- |\n| Baseline | Compare contract, requirements, and meeting notes |  |  | %s |\n| Requirements | Confirm features, conditions, and exclusions |  |  |  |\n| Delivery | Confirm acceptance criteria |  |  |  |\n\n## Approval Targets\n- WBS candidates\n- TODO candidates\n- Schedule candidates".formatted(note);
            default -> "# Project Brief\n\n## Project Purpose\n- Summarize the project goal from client materials.\n\n## Scope\n- Included:\n- Excluded:\n\n## Deliverables\n- Final deliverables:\n- Interim shares:\n\n## Timeline\n- Start:\n- Checkpoint:\n- Final delivery:\n\n## Client and References\n- Client:\n- References: %s\n\n## Items To Confirm\n- Confirm conflicting schedule, scope, and acceptance criteria.".formatted(note);
        };
    }

    private String japaneseDraftContent(String documentType, String instruction) {
        String note = instruction.isBlank() ? "資料を確認して空欄を埋めてください。" : instruction;
        return switch (documentType) {
            case "CLARIFICATION_ITEMS" -> "# 確認事項整理\n\n## 差分\n- 納期、範囲、検収基準を資料間で確認します。\n\n## 不足情報\n- クライアント名:\n- 最終納期:\n- 納品物:\n\n## クライアント確認文\n- 資料ごとに異なる項目の最終基準をご確認ください。\n\n## 根拠\n- %s".formatted(note);
            case "CLIENT_QUESTIONS" -> "# クライアント質問案\n\nこんにちは。\n\nプロジェクト基準をそろえるため、以下をご確認ください。\n\n1. 最終納期はどの日付を基準にすればよいでしょうか。\n2. 今回の範囲に含まれる納品物と除外項目をご確認ください。\n3. 優先度の高い機能や画面があれば教えてください。\n\nよろしくお願いいたします。\n\n## 作成基準\n- %s".formatted(note);
            case "MEETING_NOTE" -> "# 議事録整理案\n\n## 決定事項\n- 確定した範囲と日程を資料確認後に記入します。\n\n## タスク\n- 要件変更の確認\n- 納品物基準の整理\n- 次回質問の作成\n\n## 変更要件\n- 変更前:\n- 変更後:\n\n## 次の質問\n- 日程、範囲、検収基準の曖昧な項目を確認します。\n\n## 根拠\n- %s".formatted(note);
            case "WBS_TODO_PLAN" -> "# WBS/TODO 実行計画案\n\n## 作業分解\n| 作業 | 詳細 | 担当 | 期限 | 根拠 |\n| --- | --- | --- | --- | --- |\n| 基準整理 | 契約書、要件、議事録を照合 |  |  | %s |\n| 要件確定 | 機能、条件、除外範囲を整理 |  |  |  |\n| 納品準備 | 検収基準を確認 |  |  |  |\n\n## 承認後の候補\n- WBS候補\n- TODO候補\n- 日程候補".formatted(note);
            default -> "# プロジェクト基準整理\n\n## 目的\n- クライアント資料を基準にプロジェクトの目的を整理します。\n\n## 作業範囲\n- 含む範囲:\n- 除外範囲:\n\n## 納品物\n- 最終成果物:\n- 中間共有物:\n\n## 日程\n- 開始日:\n- 中間確認:\n- 最終納期:\n\n## クライアントと参考資料\n- クライアント名:\n- 参考資料: %s\n\n## 確認事項\n- 資料間で異なる日程、範囲、検収基準を確認します。".formatted(note);
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
