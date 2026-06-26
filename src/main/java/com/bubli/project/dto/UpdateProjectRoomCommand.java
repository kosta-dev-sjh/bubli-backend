package com.bubli.project.dto;

import com.bubli.project.type.ProjectRoomStatus;

public record UpdateProjectRoomCommand(
		String name,
		String clientName,
		ProjectRoomStatus status
) {
}
