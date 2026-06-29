package com.bubli.user.dto;

import com.bubli.user.type.ConsentType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserPrivacyConsentResponse(
		UUID userId,
		List<Item> items
) {
	public static UserPrivacyConsentResponse from(UserPrivacyConsentsResult result) {
		return new UserPrivacyConsentResponse(
				result.userId(),
				result.items().stream()
						.map(Item::from)
						.toList()
		);
	}

	public record Item(
			ConsentType consentType,
			boolean enabled,
			Instant updatedAt
	) {
		public static Item from(UserPrivacyConsentResult result) {
			return new Item(
					result.consentType(),
					result.enabled(),
					result.updatedAt()
			);
		}
	}
}
