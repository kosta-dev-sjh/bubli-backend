package com.bubli.user.dto;

import com.bubli.user.type.ConsentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdatePrivacyConsentsRequest(
		@NotNull @Size(min = 1) List<@Valid Item> items
) {
	public UpdatePrivacyConsentsCommand toCommand() {
		return new UpdatePrivacyConsentsCommand(
				items.stream()
						.map(item -> new UpdatePrivacyConsentsCommand.Item(
								item.consentType(),
								item.enabled()
						))
						.toList()
		);
	}

	public record Item(
			@NotNull ConsentType consentType,
			@NotNull Boolean enabled
	) {
	}
}
