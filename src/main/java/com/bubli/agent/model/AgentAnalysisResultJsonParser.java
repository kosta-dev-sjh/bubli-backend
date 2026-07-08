package com.bubli.agent.model;

import com.bubli.agent.contract.v1.AgentAnalysisResult;
import com.bubli.agent.validation.AgentAnalysisResultValidator;
import com.bubli.agent.validation.AgentContractError;
import com.bubli.agent.validation.AgentContractValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
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
            JsonNode root = strictObjectMapper.readTree(extractJsonObject(json));
            normalizeModelOutput(root);
            AgentAnalysisResult result = strictObjectMapper.treeToValue(root, AgentAnalysisResult.class);
            resultValidator.validateOrThrow(result);
            return result;
        } catch (JsonProcessingException exception) {
            String reason = exception.getOriginalMessage();
            throw new AgentContractValidationException(
                    "Agent analysis result is not readable JSON. reason=" + reason,
                    List.of(new AgentContractError("$", reason))
            );
        }
    }

    private void normalizeModelOutput(JsonNode root) {
        if (!(root instanceof ObjectNode objectNode)) {
            return;
        }
        normalizeChecklistSeverity(objectNode);
    }

    private void normalizeChecklistSeverity(ObjectNode root) {
        JsonNode checklist = root.path("analysis").path("checklist");
        if (!(checklist instanceof ArrayNode checklistArray)) {
            return;
        }
        for (JsonNode item : checklistArray) {
            if (item instanceof ObjectNode itemObject && missingText(itemObject.get("severity"))) {
                itemObject.put("severity", "MEDIUM");
            }
        }
    }

    private boolean missingText(JsonNode node) {
        return node == null || node.isNull() || !node.isTextual() || node.asText().isBlank();
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
