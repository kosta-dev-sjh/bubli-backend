package com.bubli.user.dto;

import java.util.List;
import java.util.UUID;

public record UserPrivacyConsentsResult(
		UUID userId,
		List<UserPrivacyConsentResult> items
) {
	public UserPrivacyConsentsResult {
		items = List.copyOf(items);
	}
}
