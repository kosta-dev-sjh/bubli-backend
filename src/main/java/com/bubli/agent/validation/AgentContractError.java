package com.bubli.agent.validation;

public record AgentContractError(
        String field,
        String reason
) {
}
