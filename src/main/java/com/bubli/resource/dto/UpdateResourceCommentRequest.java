package com.bubli.resource.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateResourceCommentRequest(
		@NotBlank(message = "자료 댓글 본문은 필수입니다.")
		String body
) {
	public String trimmedBody() {
		return body == null ? null : body.trim();
	}
}
