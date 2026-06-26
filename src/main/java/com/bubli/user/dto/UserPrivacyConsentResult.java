package com.bubli.user.dto;

import com.bubli.user.type.ConsentType;

import java.time.Instant;

public record UserPrivacyConsentResult(
		ConsentType consentType,
		boolean enabled,
		Instant updatedAt
) {
}
