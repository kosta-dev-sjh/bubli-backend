package com.bubli.personal.memo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMemoRequest(
		@NotBlank(message = "메모 내용은 필수입니다.")
		@Size(max = 5000, message = "메모 내용은 5000자 이하여야 합니다.")
		String body
) {

	public CreateMemoCommand toCommand() {
		return new CreateMemoCommand(body);
	}
}
