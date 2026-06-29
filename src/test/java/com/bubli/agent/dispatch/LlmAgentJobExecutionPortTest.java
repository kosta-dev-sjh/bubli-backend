package com.bubli.agent.dispatch;

import com.bubli.agent.model.AgentAnalysisResultJsonParser;
import com.bubli.agent.model.AiCallExecutor;
import com.bubli.agent.type.AgentJobType;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.agent.validation.AgentAnalysisResultValidator;
import com.bubli.resource.service.ResourceAnalysisPublicService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class LlmAgentJobExecutionPortTest {

	private ValidatorFactory validatorFactory;
	private ResourceAnalysisPublicService resourceAnalysisService;
	private ChatModel chatModel;
	private LlmAgentJobExecutionPort executionPort;

	@BeforeEach
	void setUp() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		Validator validator = validatorFactory.getValidator();
		resourceAnalysisService = mock(ResourceAnalysisPublicService.class);
		chatModel = mock(ChatModel.class);
		executionPort = new LlmAgentJobExecutionPort(
				resourceAnalysisService,
				chatModel,
				new AiCallExecutor(1, Duration.ZERO),
				new AgentAnalysisResultJsonParser(
						new ObjectMapper(),
						new AgentAnalysisResultValidator(validator)
				),
				new ObjectMapper()
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
	void analyzeResourceDelegatesToResourceAnalysisService() {
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
		verifyNoInteractions(chatModel);
	}
}
