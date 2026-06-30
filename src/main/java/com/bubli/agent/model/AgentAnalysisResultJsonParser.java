package com.bubli.agent.model;

import com.bubli.agent.contract.v1.AgentAnalysisResult;
import com.bubli.agent.validation.AgentAnalysisResultValidator;
import com.bubli.agent.validation.AgentContractError;
import com.bubli.agent.validation.AgentContractValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentAnalysisResultJsonParser {

    private final ObjectMapper strictObjectMapper;
    private final AgentAnalysisResultValidator resultValidator;

    public AgentAnalysisResultJsonParser(
            ObjectMapper objectMapper,
            AgentAnalysisResultValidator resultValidator
    ) {
        this.strictObjectMapper = objectMapper.copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.resultValidator = resultValidator;
    }

    public AgentAnalysisResult parse(String json) {
        if (json == null || json.isBlank()) {
            throw new AgentContractValidationException(
                    "Agent analysis result JSON is empty.",
                    List.of(new AgentContractError("$", "JSON string is required."))
            );
        }

        try {
            AgentAnalysisResult result = strictObjectMapper.readValue(extractJsonObject(json), AgentAnalysisResult.class);
            resultValidator.validateOrThrow(result);
            return result;
        } catch (JsonProcessingException exception) {
            throw new AgentContractValidationException(
                    "Agent analysis result is not readable JSON.",
                    List.of(new AgentContractError("$", exception.getOriginalMessage()))
            );
        }
    }

    private String extractJsonObject(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json|JSON)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "").trim();
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return trimmed;
        }
        return trimmed.substring(start, end + 1);
    }
}
