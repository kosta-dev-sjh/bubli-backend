package com.bubli.agent.validation;

import com.bubli.agent.contract.v1.AgentAnalysisResult;
import com.bubli.agent.contract.v1.Suggestion;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AgentAnalysisResultValidator {

    private final Validator validator;

    public List<AgentContractError> validate(AgentAnalysisResult result) {
        if (result == null) {
            return List.of(new AgentContractError("$", "분석 결과가 null입니다."));
        }

        List<AgentContractError> errors = new ArrayList<>(
                validator.validate(result).stream()
                        .map(this::toError)
                        .toList()
        );

        if (!AgentAnalysisResult.SCHEMA_VERSION.equals(result.schemaVersion())) {
            errors.add(new AgentContractError(
                    "schemaVersion",
                    "지원하는 버전은 " + AgentAnalysisResult.SCHEMA_VERSION + "입니다."
            ));
        }

        if (result.suggestions() != null) {
            for (int index = 0; index < result.suggestions().size(); index++) {
                validateSuggestion(result.suggestions().get(index), index, errors);
            }
        }

        return errors.stream()
                .distinct()
                .sorted(Comparator.comparing(AgentContractError::field))
                .toList();
    }

    public void validateOrThrow(AgentAnalysisResult result) {
        List<AgentContractError> errors = validate(result);
        if (!errors.isEmpty()) {
            throw new AgentContractValidationException("에이전트 분석 결과가 analysis.v1 계약을 위반했습니다.", errors);
        }
    }

    private void validateSuggestion(
            Suggestion suggestion,
            int index,
            List<AgentContractError> errors
    ) {
        if (suggestion == null || suggestion.type() == null) {
            return;
        }

        String path = "suggestions[" + index + "]";
        switch (suggestion.type()) {
            case TASK -> {
                requireText(suggestion.title(), path + ".title", "TASK 제안에는 title이 필요합니다.", errors);
                requireText(
                        suggestion.sourceText(),
                        path + ".sourceText",
                        "TASK 제안에는 근거가 되는 sourceText가 필요합니다.",
                        errors
                );
            }
            case REQUIREMENT -> {
                requireText(
                        suggestion.title(),
                        path + ".title",
                        "REQUIREMENT 제안에는 title이 필요합니다.",
                        errors
                );
                requireText(
                        suggestion.description(),
                        path + ".description",
                        "REQUIREMENT 제안에는 description이 필요합니다.",
                        errors
                );
            }
            case CONTRACT_FIELD -> {
                requireText(
                        suggestion.fieldKey(),
                        path + ".fieldKey",
                        "CONTRACT_FIELD 제안에는 fieldKey가 필요합니다.",
                        errors
                );
                requireText(
                        suggestion.value(),
                        path + ".value",
                        "CONTRACT_FIELD 제안에는 value가 필요합니다.",
                        errors
                );
            }
        }
    }

    private void requireText(
            String value,
            String field,
            String reason,
            List<AgentContractError> errors
    ) {
        if (value == null || value.isBlank()) {
            errors.add(new AgentContractError(field, reason));
        }
    }

    private AgentContractError toError(ConstraintViolation<AgentAnalysisResult> violation) {
        return new AgentContractError(violation.getPropertyPath().toString(), violation.getMessage());
    }
}
