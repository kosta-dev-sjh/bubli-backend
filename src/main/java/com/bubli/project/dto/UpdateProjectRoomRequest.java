package com.bubli.project.dto;

import com.bubli.project.type.ProjectRoomStatus;
import jakarta.validation.constraints.Size;

public record UpdateProjectRoomRequest(
		@Size(min = 1, max = 120, message = "프로젝트룸 이름은 1자 이상 120자 이하여야 합니다.")
		String name,

		@Size(max = 120, message = "클라이언트명은 120자 이하여야 합니다.")
		String clientName,

		ProjectRoomStatus status
) {
	public UpdateProjectRoomCommand toCommand() {
		return new UpdateProjectRoomCommand(
				name == null ? null : name.trim(),
				clientName == null ? null : clientName.trim(),
				status
		);
	}
}
