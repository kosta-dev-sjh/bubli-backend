package com.bubli.resource.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateResourceCommentRequest(
		UUID parentId,

		@NotBlank(message = "자료 댓글 본문은 필수입니다.")
		String body
) {
	public String trimmedBody() {
		return body == null ? null : body.trim();
	}
}
