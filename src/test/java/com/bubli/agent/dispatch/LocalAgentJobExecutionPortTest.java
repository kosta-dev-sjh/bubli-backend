package com.bubli.agent.dispatch;

import com.bubli.agent.type.AgentJobType;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.resource.service.ResourceAnalysisPublicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LocalAgentJobExecutionPortTest {

    @Test
    void analyzesResourceJob() {
        ResourceAnalysisPublicService resourceAnalysisService = mock(ResourceAnalysisPublicService.class);
        LocalAgentJobExecutionPort executionPort = new LocalAgentJobExecutionPort(
                resourceAnalysisService,
                new ObjectMapper()
        );
        UUID jobId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        var outcome = executionPort.execute(new AgentJobQueueMessage(
                jobId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                resourceId,
                AgentJobType.ANALYZE_RESOURCE,
                Instant.now()
        ));

        assertThat(outcome).isPresent();
        assertThat(outcome.get().successful()).isTrue();
        verify(resourceAnalysisService).analyzeResourceForJob(resourceId, jobId);
    }

    @Test
    void marksResourceFailedWhenAnalyzeResourceThrows() {
        ResourceAnalysisPublicService resourceAnalysisService = mock(ResourceAnalysisPublicService.class);
        LocalAgentJobExecutionPort executionPort = new LocalAgentJobExecutionPort(
                resourceAnalysisService,
                new ObjectMapper()
        );
        UUID jobId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Extracted text is empty."))
                .when(resourceAnalysisService).analyzeResourceForJob(resourceId, jobId);

        var outcome = executionPort.execute(new AgentJobQueueMessage(
                jobId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                resourceId,
                AgentJobType.ANALYZE_RESOURCE,
                Instant.now()
        ));

        assertThat(outcome).isPresent();
        assertThat(outcome.get().successful()).isFalse();
        assertThat(outcome.get().errorCode()).isEqualTo("AGENT_EXECUTION_FAILED");
        assertThat(outcome.get().errorMessage()).isEqualTo("Extracted text is empty.");
        verify(resourceAnalysisService).markAnalysisFailed(resourceId);
    }

    @Test
    void generatesTaskSuggestionDraft() {
        LocalAgentJobExecutionPort executionPort = new LocalAgentJobExecutionPort(
                mock(ResourceAnalysisPublicService.class),
                new ObjectMapper()
        );

        var outcome = executionPort.execute(new AgentJobQueueMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                AgentJobType.GENERATE_TASKS,
                Instant.now()
        ));

        assertThat(outcome).isPresent();
        assertThat(outcome.get().successful()).isTrue();
        assertThat(outcome.get().suggestionDrafts()).hasSize(1);
        assertThat(outcome.get().suggestionDrafts().getFirst().suggestionType()).isEqualTo(AgentSuggestionType.TASK);
        assertThat(outcome.get().modelCallLogs()).hasSize(1);
    }

    @Test
    void generatesEnglishLocalFallbackWhenLocaleIsEnglish() {
        LocalAgentJobExecutionPort executionPort = new LocalAgentJobExecutionPort(
                mock(ResourceAnalysisPublicService.class),
                new ObjectMapper()
        );

        var outcome = executionPort.execute(new AgentJobQueueMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                AgentJobType.GENERATE_TASKS,
                Map.of("locale", "en-US"),
                Instant.now()
        ));

        assertThat(outcome).isPresent();
        assertThat(outcome.get().successful()).isTrue();
        assertThat(outcome.get().suggestionDrafts().getFirst().payloadJson())
                .contains("Task candidate", "Created a task candidate.");
    }

    @Test
    void generatesJapaneseLocalFallbackWhenLocaleIsJapanese() {
        LocalAgentJobExecutionPort executionPort = new LocalAgentJobExecutionPort(
                mock(ResourceAnalysisPublicService.class),
                new ObjectMapper()
        );

        var outcome = executionPort.execute(new AgentJobQueueMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                AgentJobType.DRAFT_DOCUMENT,
                Map.of("locale", "ja-JP"),
                Instant.now()
        ));

        assertThat(outcome).isPresent();
        assertThat(outcome.get().successful()).isTrue();
        assertThat(outcome.get().suggestionDrafts().getFirst().payloadJson())
                .contains("文書ドラフト候補", "# 文書ドラフト");
    }

    @Test
    void generatesDailySummarySuggestionWithRequestedSummaryDate() {
        LocalAgentJobExecutionPort executionPort = new LocalAgentJobExecutionPort(
                mock(ResourceAnalysisPublicService.class),
                new ObjectMapper()
        );

        var outcome = executionPort.execute(new AgentJobQueueMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                AgentJobType.DAILY_SUMMARY,
                Map.of("summaryDate", "2026-07-01"),
                Instant.now()
        ));

        assertThat(outcome).isPresent();
        assertThat(outcome.get().successful()).isTrue();
        assertThat(outcome.get().suggestionDrafts()).hasSize(1);
        assertThat(outcome.get().suggestionDrafts().getFirst().suggestionType()).isEqualTo(AgentSuggestionType.DAILY_SUMMARY);
        assertThat(outcome.get().suggestionDrafts().getFirst().payloadJson()).contains("2026-07-01", "summaryJson");
    }

    @Test
    void preservesWbsScheduleHintsFromRequestPayload() {
        LocalAgentJobExecutionPort executionPort = new LocalAgentJobExecutionPort(
                mock(ResourceAnalysisPublicService.class),
                new ObjectMapper()
        );

        var outcome = executionPort.execute(new AgentJobQueueMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                AgentJobType.GENERATE_WBS,
                Map.of(
                        "scheduleTitle", "중간 리뷰 준비",
                        "startsAt", "2026-07-10T01:00:00Z",
                        "endsAt", "2026-07-10T02:00:00Z",
                        "allDay", false
                ),
                Instant.now()
        ));

        assertThat(outcome).isPresent();
        assertThat(outcome.get().successful()).isTrue();
        assertThat(outcome.get().suggestionDrafts()).hasSize(1);
        assertThat(outcome.get().suggestionDrafts().getFirst().suggestionType()).isEqualTo(AgentSuggestionType.WBS);
        assertThat(outcome.get().suggestionDrafts().getFirst().payloadJson())
                .contains("scheduleTitle", "중간 리뷰 준비", "startsAt", "2026-07-10T01:00:00Z");
    }

    @Test
    void generatesDocumentDraftSuggestionWithRequestPayload() {
        LocalAgentJobExecutionPort executionPort = new LocalAgentJobExecutionPort(
                mock(ResourceAnalysisPublicService.class),
                new ObjectMapper()
        );
        UUID sourceResourceId = UUID.randomUUID();

        var outcome = executionPort.execute(new AgentJobQueueMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                AgentJobType.DRAFT_DOCUMENT,
                Map.of(
                        "documentType", "proposal",
                        "instruction", "견적서 초안",
                        "sourceResourceIds", List.of(sourceResourceId.toString())
                ),
                Instant.now()
        ));

        assertThat(outcome).isPresent();
        assertThat(outcome.get().successful()).isTrue();
        assertThat(outcome.get().suggestionDrafts()).hasSize(1);
        assertThat(outcome.get().suggestionDrafts().getFirst().suggestionType()).isEqualTo(AgentSuggestionType.DOCUMENT_DRAFT);
        assertThat(outcome.get().suggestionDrafts().getFirst().payloadJson())
                .contains("proposal", "견적서 초안", sourceResourceId.toString(), "contentMarkdown");
    }
}
