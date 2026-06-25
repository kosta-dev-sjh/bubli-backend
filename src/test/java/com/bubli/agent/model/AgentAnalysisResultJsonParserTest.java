package com.bubli.agent.model;

import com.bubli.agent.contract.v1.AgentAnalysisResult;
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
        assertThat(result.analysis().summary()).contains("외주 계약서");
        assertThat(result.suggestions()).hasSize(3);
        assertThat(result.suggestions().get(0).type()).isEqualTo(SuggestionType.TASK);
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
                .hasMessageContaining("JSON을 읽을 수 없습니다");
    }

    private static String readFixture(String filename) throws IOException {
        String path = "/fixtures/agent/" + filename;
        try (InputStream inputStream = AgentAnalysisResultJsonParserTest.class.getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IOException("Fixture를 찾을 수 없습니다: " + path);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
