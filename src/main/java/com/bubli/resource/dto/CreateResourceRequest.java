package com.bubli.resource.dto;

import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateResourceRequest(
		@NotBlank(message = "자료 제목은 필수입니다.")
		@Size(max = 200, message = "자료 제목은 200자 이하여야 합니다.")
		String title,

		@NotNull(message = "자료 종류는 필수입니다.")
		ResourceKind kind,

		@NotNull(message = "자료 공개 범위는 필수입니다.")
		ResourceVisibility visibility,

		UUID roomId
) {
	public CreateResourceCommand toCommand() {
		return new CreateResourceCommand(
				title == null ? null : title.trim(),
				kind,
				visibility,
				roomId
		);
	}
}
