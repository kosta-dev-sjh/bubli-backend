package com.bubli.user.dto;

import com.bubli.user.type.ConsentType;

import java.util.List;

public record UpdatePrivacyConsentsCommand(
		List<Item> items
) {
	public UpdatePrivacyConsentsCommand {
		items = List.copyOf(items);
	}

	public record Item(
			ConsentType consentType,
			boolean enabled
	) {
	}
}
