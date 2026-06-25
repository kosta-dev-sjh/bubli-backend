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
                    "에이전트 분석 결과 JSON이 비어 있습니다.",
                    List.of(new AgentContractError("$", "JSON 문자열이 필요합니다."))
            );
        }

        try {
            AgentAnalysisResult result = strictObjectMapper.readValue(json, AgentAnalysisResult.class);
            resultValidator.validateOrThrow(result);
            return result;
        } catch (JsonProcessingException exception) {
            throw new AgentContractValidationException(
                    "에이전트 분석 결과 JSON을 읽을 수 없습니다.",
                    List.of(new AgentContractError("$", exception.getOriginalMessage()))
            );
        }
    }
}
