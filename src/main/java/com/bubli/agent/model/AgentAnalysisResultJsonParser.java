package com.bubli.agent.model;

import com.bubli.agent.contract.v1.AgentAnalysisResult;
import com.bubli.agent.validation.AgentAnalysisResultValidator;
import com.bubli.agent.validation.AgentContractError;
import com.bubli.agent.validation.AgentContractValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.springframework.stereotype.Component;

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
            JsonNode root = readTree(json);
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
        normalizeSuggestionFields(objectNode);
    }

    private void normalizeChecklistSeverity(ObjectNode root) {
        JsonNode checklist = root.path("analysis").path("checklist");
        if (!(checklist instanceof ArrayNode checklistArray)) {
            return;
        }
        for (JsonNode item : checklistArray) {
            if (item instanceof ObjectNode itemObject) {
                itemObject.remove("items");
                if (missingText(itemObject.get("severity"))) {
                    itemObject.put("severity", "MEDIUM");
                } else {
                    normalizeEnumText(itemObject, "severity");
                }
            }
        }
    }

    private void normalizeSuggestionFields(ObjectNode root) {
        JsonNode suggestions = root.path("suggestions");
        if (!(suggestions instanceof ArrayNode suggestionsArray)) {
            return;
        }
        for (JsonNode item : suggestionsArray) {
            if (!(item instanceof ObjectNode suggestion)) {
                continue;
            }
            normalizeEnumText(suggestion, "type");
            normalizeEnumText(suggestion, "status");
            normalizeSuggestionStatus(suggestion);
            normalizeConfidence(suggestion);
            normalizeUuidText(suggestion, "assigneeUserId");
            normalizeUuidText(suggestion, "wbsItemId");
            normalizeUuidText(suggestion, "parentId");
            normalizeInstantText(suggestion, "startsAt");
            normalizeInstantText(suggestion, "dueAt");
            normalizeInstantText(suggestion, "endsAt");
        }
    }

    private void normalizeSuggestionStatus(ObjectNode suggestion) {
        JsonNode status = suggestion.get("status");
        if (status == null || !status.isTextual() || status.asText().isBlank()) {
            return;
        }
        String type = suggestion.path("type").asText();
        String value = status.asText();
        if ("TASK".equals(type) && isOneOf(value, "TODO", "IN_PROGRESS", "REVIEW", "DONE", "BLOCKED")) {
            return;
        }
        if ("WBS".equals(type) && isOneOf(value, "TODO", "IN_PROGRESS", "DONE")) {
            return;
        }
        suggestion.remove("status");
    }

    private boolean isOneOf(String value, String... candidates) {
        for (String candidate : candidates) {
            if (candidate.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private void normalizeEnumText(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            return;
        }
        String normalized = value.asText()
                .trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase();
        if (!normalized.isBlank()) {
            node.put(field, normalized);
        }
    }

    private void normalizeConfidence(ObjectNode suggestion) {
        JsonNode confidence = suggestion.get("confidence");
        if (confidence == null || confidence.isNull()) {
            return;
        }
        double value;
        if (confidence.isNumber()) {
            value = confidence.asDouble();
        } else if (confidence.isTextual()) {
            try {
                value = Double.parseDouble(confidence.asText().trim().replace("%", ""));
            } catch (NumberFormatException exception) {
                suggestion.remove("confidence");
                return;
            }
        } else {
            suggestion.remove("confidence");
            return;
        }
        if (value > 1.0 && value <= 100.0) {
            value = value / 100.0;
        }
        if (value < 0.0) {
            value = 0.0;
        }
        if (value > 1.0) {
            value = 1.0;
        }
        suggestion.put("confidence", value);
    }

    private void normalizeUuidText(ObjectNode suggestion, String field) {
        JsonNode value = suggestion.get(field);
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isTextual() || value.asText().isBlank()) {
            suggestion.remove(field);
            return;
        }
        String text = value.asText().trim();
        if (!text.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            suggestion.remove(field);
            return;
        }
        suggestion.put(field, text);
    }

    private void normalizeInstantText(ObjectNode suggestion, String field) {
        JsonNode value = suggestion.get(field);
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isTextual() || value.asText().isBlank()) {
            suggestion.remove(field);
            return;
        }
        String text = value.asText().trim();
        if (text.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            suggestion.put(field, text + "T00:00:00Z");
        } else {
            suggestion.put(field, text);
        }
    }

    private boolean missingText(JsonNode node) {
        return node == null || node.isNull() || !node.isTextual() || node.asText().isBlank();
    }

    private JsonNode readTree(String json) throws JsonProcessingException {
        String extracted = extractJsonObject(json);
        try {
            return strictObjectMapper.readTree(extracted);
        } catch (JsonProcessingException exception) {
            String repaired = repairMissingClosingQuoteBeforeComma(extracted);
            if (repaired.equals(extracted)) {
                throw exception;
            }
            return strictObjectMapper.readTree(repaired);
        }
    }

    private String repairMissingClosingQuoteBeforeComma(String json) {
        String[] lines = json.split("\\R", -1);
        boolean changed = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (!trimmed.endsWith(",") || trimmed.endsWith("\",")) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String value = line.substring(colon + 1).trim();
            if (!value.startsWith("\"") || unescapedQuoteCount(line) % 2 == 0) {
                continue;
            }
            lines[i] = line.substring(0, line.lastIndexOf(',')) + "\",";
            changed = true;
        }
        return changed ? String.join(System.lineSeparator(), lines) : json;
    }

    private int unescapedQuoteCount(String value) {
        int count = 0;
        boolean escaped = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '"' && !escaped) {
                count++;
            }
            escaped = current == '\\' && !escaped;
            if (current != '\\') {
                escaped = false;
            }
        }
        return count;
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
