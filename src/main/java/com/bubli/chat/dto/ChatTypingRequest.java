package com.bubli.chat.dto;

import jakarta.validation.constraints.NotNull;

public record ChatTypingRequest(
		@NotNull
		Boolean typing
) {
}
