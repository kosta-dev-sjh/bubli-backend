package com.bubli.agent.contract.v1;

import jakarta.validation.constraints.NotBlank;

public record ModelInfo(
        @NotBlank String name,
        @NotBlank String promptVersion
) {
}
