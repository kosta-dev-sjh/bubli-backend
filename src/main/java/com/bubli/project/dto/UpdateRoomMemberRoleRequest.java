package com.bubli.project.dto;

import com.bubli.project.type.RoomMemberRole;
import jakarta.validation.constraints.NotNull;

public record UpdateRoomMemberRoleRequest(
		@NotNull(message = "멤버 역할은 필수입니다.")
		RoomMemberRole role
) {
}
