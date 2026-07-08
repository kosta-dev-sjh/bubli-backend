package com.bubli.agent.model;

import com.bubli.agent.contract.v1.AgentAnalysisResult;
import com.bubli.agent.contract.v1.ChecklistSeverity;
import com.bubli.agent.contract.v1.SuggestionType;
import com.bubli.agent.validation.AgentAnalysisResultValidator;
import com.bubli.agent.validation.AgentContractValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentAnalysisResultJsonParserTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static AgentAnalysisResultJsonParser parser;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();
        parser = new AgentAnalysisResultJsonParser(
                new ObjectMapper(),
                new AgentAnalysisResultValidator(validator)
        );
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void parsesValidAnalysisV1Fixture() throws Exception {
        AgentAnalysisResult result = parser.parse(readFixture("analysis-v1-valid.json"));

        assertThat(result.schemaVersion()).isEqualTo(AgentAnalysisResult.SCHEMA_VERSION);
        assertThat(result.suggestions()).hasSize(3);
        assertThat(result.suggestions().getFirst().type()).isEqualTo(SuggestionType.TASK);
    }

    @Test
    void rejectsUnsupportedVersionAndInvalidSuggestionFields() throws Exception {
        assertThatThrownBy(() -> parser.parse(readFixture("analysis-v1-invalid.json")))
                .isInstanceOf(AgentContractValidationException.class)
                .satisfies(exception -> {
                    AgentContractValidationException validationException =
                            (AgentContractValidationException) exception;
                    assertThat(validationException.getErrors())
                            .extracting(error -> error.field())
                            .contains(
                                    "schemaVersion",
                                    "suggestions[0].title",
                                    "suggestions[0].sourceText",
                                    "suggestions[1].value"
                            );
                });
    }

    @Test
    void rejectsUnknownJsonFields() {
        String json = """
                {
                  "schemaVersion": "analysis.v1",
                  "resourceId": "24cf02d3-eb51-4a2c-86f9-428feece0ce6",
                  "unexpected": true,
                  "model": {"name": "test", "promptVersion": "p1"},
                  "analysis": {
                    "summary": "summary",
                    "keywords": [],
                    "risks": [],
                    "checklist": []
                  },
                  "suggestions": []
                }
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AgentContractValidationException.class)
                .hasMessageContaining("not readable JSON");
    }

    @Test
    void extractsJsonObjectFromMarkdownFencedResponse() {
        String response = """
                아래는 요청하신 결과입니다.

                ```json
                {
                  "schemaVersion": "analysis.v1",
                  "resourceId": "24cf02d3-eb51-4a2c-86f9-428feece0ce6",
                  "model": {"name": "test", "promptVersion": "p1"},
                  "analysis": {
                    "summary": "작업 제안 요약",
                    "keywords": ["작업"],
                    "risks": [],
                    "checklist": []
                  },
                  "suggestions": [
                    {
                      "type": "TASK",
                      "title": "로그인 API 구현",
                      "description": "JWT 기반 로그인 API를 구현합니다.",
                      "sourceText": "인증 기능이 필요합니다.",
                      "confidence": 0.9
                    }
                  ]
                }
                ```
                """;

        AgentAnalysisResult result = parser.parse(response);

        assertThat(result.schemaVersion()).isEqualTo(AgentAnalysisResult.SCHEMA_VERSION);
        assertThat(result.suggestions()).hasSize(1);
        assertThat(result.suggestions().getFirst().type()).isEqualTo(SuggestionType.TASK);
    }

    @Test
    void parsesModelJsonWithUnescapedLineBreaksInsideStringValues() {
        String response = """
                {
                  "schemaVersion": "analysis.v1",
                  "resourceId": "24cf02d3-eb51-4a2c-86f9-428feece0ce6",
                  "model": {"name": "test", "promptVersion": "p1"},
                  "analysis": {
                    "summary": "작업 제안 요약",
                    "keywords": ["작업"],
                    "risks": [],
                    "checklist": []
                  },
                  "suggestions": [
                    {
                      "type": "REVIEW_ITEM",
                      "title": "계약 조건 확인",
                      "description": "계약 조건을 검토합니다.",
                      "sourceText": "첫 번째 근거 문장입니다.
                두 번째 근거 문장입니다.",
                      "confidence": 0.8
                    }
                  ]
                }
                """;

        AgentAnalysisResult result = parser.parse(response);

        assertThat(result.suggestions()).hasSize(1);
        assertThat(result.suggestions().getFirst().sourceText()).contains("두 번째 근거");
    }

    @Test
    void defaultsMissingChecklistSeverityForDocumentDraftModelOutput() {
        String response = """
                {
                  "schemaVersion": "analysis.v1",
                  "resourceId": "44721708-84b3-4cdc-86cd-f403c1efa37a",
                  "model": {"name": "spring-ai-chat", "promptVersion": "agent-job-llm-v1"},
                  "analysis": {
                    "summary": "문서 초안 요약",
                    "keywords": ["문서 초안"],
                    "risks": [],
                    "checklist": [
                      {"title": "자료 처리 실패 시 대응 방안 마련"}
                    ]
                  },
                  "suggestions": [
                    {
                      "type": "DOCUMENT_DRAFT",
                      "title": "요구사항 문서 초안",
                      "description": "요구사항을 정리한 문서 초안입니다.",
                      "sourceText": "요구사항 문서를 바탕으로 작성합니다.",
                      "confidence": 0.8,
                      "startsAt": "2026-07-07T15:00:00Z",
                      "dueAt": "2026-07-29T14:59:59.999Z",
                      "endsAt": "2026-07-29T14:59:59.999Z",
                      "documentType": "WBS_TODO_PLAN",
                      "contentMarkdown": "# 요구사항 문서 초안\\n\\n## 개요\\n문서 초안 내용"
                    }
                  ]
                }
                """;

        AgentAnalysisResult result = parser.parse(response);

        assertThat(result.analysis().checklist().getFirst().severity()).isEqualTo(ChecklistSeverity.MEDIUM);
        assertThat(result.suggestions()).hasSize(1);
        assertThat(result.suggestions().getFirst().type()).isEqualTo(SuggestionType.DOCUMENT_DRAFT);
        assertThat(result.suggestions().getFirst().contentMarkdown()).contains("# 요구사항 문서 초안");
    }

    private static String readFixture(String filename) throws IOException {
        String path = "/fixtures/agent/" + filename;
        try (InputStream inputStream = AgentAnalysisResultJsonParserTest.class.getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IOException("Fixture not found: " + path);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
