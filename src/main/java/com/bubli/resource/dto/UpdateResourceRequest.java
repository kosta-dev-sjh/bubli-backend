package com.bubli.resource.dto;

import jakarta.validation.constraints.Size;

public record UpdateResourceRequest(
		@Size(max = 200, message = "자료 제목은 200자 이하여야 합니다.")
		String title
) {
	public String trimmedTitle() {
		return title == null ? null : title.trim();
	}
}
