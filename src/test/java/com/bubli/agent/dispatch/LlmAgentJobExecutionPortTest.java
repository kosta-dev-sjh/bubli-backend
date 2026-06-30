package com.bubli.agent.dispatch;

import com.bubli.agent.model.AgentAnalysisResultJsonParser;
import com.bubli.agent.model.AiCallExecutor;
import com.bubli.agent.dto.AgentJobContext;
import com.bubli.agent.service.AgentJobContextCollector;
import com.bubli.agent.type.AgentJobType;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.agent.validation.AgentAnalysisResultValidator;
import com.bubli.resource.dto.ResourceAnalysisPage;
import com.bubli.resource.dto.ResourceAnalysisSource;
import com.bubli.resource.service.ResourceAnalysisPublicService;
import com.bubli.resource.type.DocumentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class LlmAgentJobExecutionPortTest {

	private ValidatorFactory validatorFactory;
	private ResourceAnalysisPublicService resourceAnalysisService;
	private ChatModel chatModel;
	private AgentJobContextCollector contextCollector;
	private LlmAgentJobExecutionPort executionPort;

	@BeforeEach
	void setUp() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		Validator validator = validatorFactory.getValidator();
		resourceAnalysisService = mock(ResourceAnalysisPublicService.class);
		chatModel = mock(ChatModel.class);
		contextCollector = mock(AgentJobContextCollector.class);
		given(contextCollector.collect(org.mockito.ArgumentMatchers.any(AgentJobQueueMessage.class)))
				.willReturn(new AgentJobContext("[Room tasks]\n- existing task", 28));
		executionPort = new LlmAgentJobExecutionPort(
				resourceAnalysisService,
				chatModel,
				new AiCallExecutor(1, Duration.ZERO),
				new AgentAnalysisResultJsonParser(
						new ObjectMapper(),
						new AgentAnalysisResultValidator(validator)
				),
				new ObjectMapper(),
				contextCollector
		);
	}

	@AfterEach
	void tearDown() {
		validatorFactory.close();
	}

	@Test
	void generatesSuggestionDraftsFromLlmContractResult() {
		UUID jobId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		given(chatModel.call(contains("GENERATE_TASKS"))).willReturn("""
				{
				  "schemaVersion": "analysis.v1",
				  "resourceId": "00000000-0000-0000-0000-000000000000",
				  "model": {"name": "bedrock-test", "promptVersion": "prompt-test"},
				  "analysis": {
				    "summary": "작업 후보를 생성했습니다.",
				    "keywords": ["task"],
				    "risks": [],
				    "checklist": []
				  },
				  "suggestions": [
				    {
				      "type": "TASK",
				      "title": "로그인 API 구현",
				      "description": "JWT 기반 로그인 API를 구현합니다.",
				      "sourceText": "인증 기능이 필요합니다.",
				      "confidence": 0.91
				    }
				  ]
				}
				""");

		var outcome = executionPort.execute(new AgentJobQueueMessage(
				jobId,
				userId,
				roomId,
				null,
				AgentJobType.GENERATE_TASKS,
				Instant.now()
		));

		assertThat(outcome).isPresent();
		assertThat(outcome.get().successful()).isTrue();
		assertThat(outcome.get().suggestionDrafts()).hasSize(1);
		AgentJobExecutionSuggestionDraft draft = outcome.get().suggestionDrafts().getFirst();
		assertThat(draft.suggestionType()).isEqualTo(AgentSuggestionType.TASK);
		assertThat(draft.payloadJson()).contains("로그인 API 구현", "LLM");
		assertThat(draft.evidenceJson()).contains("prompt-test", "작업 후보를 생성했습니다.");
		assertThat(outcome.get().modelCallLogs()).hasSize(1);
		assertThat(outcome.get().modelCallLogs().getFirst().errorCode()).isNull();
		verifyNoInteractions(resourceAnalysisService);
	}

	@Test
	void returnsInvalidOutputFailureWhenLlmResponseViolatesSchema() {
		given(chatModel.call(contains("GENERATE_REQUIREMENTS"))).willReturn("{\"schemaVersion\":\"wrong\"}");

		var outcome = executionPort.execute(new AgentJobQueueMessage(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				null,
				AgentJobType.GENERATE_REQUIREMENTS,
				Instant.now()
		));

		assertThat(outcome).isPresent();
		assertThat(outcome.get().successful()).isFalse();
		assertThat(outcome.get().errorCode()).isEqualTo("AI_INVALID_OUTPUT");
		assertThat(outcome.get().modelCallLogs()).hasSize(1);
		assertThat(outcome.get().modelCallLogs().getFirst().errorCode()).isEqualTo("AI_INVALID_OUTPUT");
	}

	@Test
	void analyzeResourceStoresLlmAnalysisAndReturnsSuggestionDrafts() {
		UUID jobId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		ResourceAnalysisSource source = new ResourceAnalysisSource(
				resourceId,
				roomId,
				"contract.txt",
				"text/plain",
				DocumentType.CONTRACT,
				List.of(new ResourceAnalysisPage(null, "계약금은 100만원이고 검수 조건 확인이 필요합니다.")),
				"계약금은 100만원이고 검수 조건 확인이 필요합니다.",
				1,
				29
		);
		given(resourceAnalysisService.loadAnalysisSourceForJob(resourceId)).willReturn(source);
		given(chatModel.call(contains("AI document analyzer"))).willReturn("""
				{
				  "schemaVersion": "analysis.v1",
				  "resourceId": "%s",
				  "model": {"name": "bedrock-test", "promptVersion": "prompt-test"},
				  "analysis": {
				    "summary": "계약금과 검수 조건 확인이 필요한 계약 문서입니다.",
				    "keywords": ["계약금", "검수"],
				    "risks": ["검수 조건이 구체적으로 확정되지 않았습니다."],
				    "checklist": [{"title": "검수 기준과 완료일을 확인한다.", "severity": "MEDIUM"}]
				  },
				  "suggestions": [
				    {
				      "type": "REVIEW_ITEM",
				      "title": "검수 조건 확인",
				      "description": "계약 승인 전 검수 기준과 완료일을 확인합니다.",
				      "sourceText": "검수 조건 확인이 필요합니다.",
				      "confidence": 0.88
				    },
				    {
				      "type": "CONTRACT_FIELD",
				      "title": "계약금 추출",
				      "description": "문서에 명시된 계약금입니다.",
				      "sourceText": "계약금은 100만원",
				      "confidence": 0.91,
				      "fieldKey": "contract_amount",
				      "value": "100만원"
				    }
				  ]
				}
				""".formatted(resourceId));

		var outcome = executionPort.execute(new AgentJobQueueMessage(
				jobId,
				userId,
				roomId,
				resourceId,
				AgentJobType.ANALYZE_RESOURCE,
				Instant.now()
		));

		assertThat(outcome).isPresent();
		assertThat(outcome.get().successful()).isTrue();
		assertThat(outcome.get().suggestionDrafts()).hasSize(2);
		assertThat(outcome.get().suggestionDrafts().getFirst().suggestionType())
				.isEqualTo(AgentSuggestionType.REVIEW_ITEM);
		assertThat(outcome.get().suggestionDrafts().get(1).suggestionType())
				.isEqualTo(AgentSuggestionType.CONTRACT_FIELD);
		assertThat(outcome.get().suggestionDrafts().get(1).payloadJson())
				.contains("contract_amount", "100만원", "LLM");
		verify(resourceAnalysisService).loadAnalysisSourceForJob(resourceId);
		verify(resourceAnalysisService).completeAnalysisForJob(
				eq(source),
				eq(jobId),
				argThat(analysis -> analysis.get("summary").equals("계약금과 검수 조건 확인이 필요한 계약 문서입니다.")
						&& analysis.get("documentType").equals("CONTRACT")
						&& analysis.get("suggestionCount").equals(2))
		);
	}

	@Test
	void analyzeResourceMarksFailedWhenLlmResponseViolatesSchema() {
		UUID resourceId = UUID.randomUUID();
		ResourceAnalysisSource source = new ResourceAnalysisSource(
				resourceId,
				UUID.randomUUID(),
				"contract.txt",
				"text/plain",
				DocumentType.CONTRACT,
				List.of(new ResourceAnalysisPage(null, "계약서 본문")),
				"계약서 본문",
				1,
				6
		);
		given(resourceAnalysisService.loadAnalysisSourceForJob(resourceId)).willReturn(source);
		given(chatModel.call(contains("AI document analyzer"))).willReturn("{\"schemaVersion\":\"wrong\"}");

		var outcome = executionPort.execute(new AgentJobQueueMessage(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				resourceId,
				AgentJobType.ANALYZE_RESOURCE,
				Instant.now()
		));

		assertThat(outcome).isPresent();
		assertThat(outcome.get().successful()).isFalse();
		assertThat(outcome.get().errorCode()).isEqualTo("AI_INVALID_OUTPUT");
		verify(resourceAnalysisService).markAnalysisFailed(resourceId);
	}
}
