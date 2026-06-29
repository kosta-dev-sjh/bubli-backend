package com.bubli.project.dto;

public record CreateInviteLinkRequest(
		int expiresInHours
) {
	public CreateInviteLinkRequest {
		if (expiresInHours <= 0) {
			expiresInHours = 72;
		}
	}
}
