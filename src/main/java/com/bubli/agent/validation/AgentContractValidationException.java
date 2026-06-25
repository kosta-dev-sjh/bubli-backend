package com.bubli.agent.validation;

import java.util.List;

public class AgentContractValidationException extends RuntimeException {

    private final List<AgentContractError> errors;

    public AgentContractValidationException(String message, List<AgentContractError> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public List<AgentContractError> getErrors() {
        return errors;
    }
}
