package com.bubli.voice.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMicStatusRequest(
        @NotBlank String micStatus
) {
}
