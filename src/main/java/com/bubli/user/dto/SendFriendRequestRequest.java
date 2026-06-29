package com.bubli.user.dto;

import jakarta.validation.constraints.NotBlank;

public record SendFriendRequestRequest(
		@NotBlank String bubliId
) {
}
