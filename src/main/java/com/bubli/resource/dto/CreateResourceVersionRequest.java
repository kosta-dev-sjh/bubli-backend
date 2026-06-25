package com.bubli.resource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateResourceVersionRequest(
		@NotBlank(message = "저장소 키는 필수입니다.")
		@Size(max = 500, message = "저장소 키는 500자 이하여야 합니다.")
		String storageKey,

		@NotBlank(message = "원본 파일명은 필수입니다.")
		@Size(max = 255, message = "원본 파일명은 255자 이하여야 합니다.")
		String originalName,

		@NotBlank(message = "MIME 타입은 필수입니다.")
		@Size(max = 120, message = "MIME 타입은 120자 이하여야 합니다.")
		String mimeType,

		@NotNull(message = "파일 크기는 필수입니다.")
		@Positive(message = "파일 크기는 1 이상이어야 합니다.")
		Long sizeBytes,

		@Size(max = 128, message = "체크섬은 128자 이하여야 합니다.")
		String checksum
) {
}
