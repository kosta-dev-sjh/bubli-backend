package com.bubli.resource.storage;

import java.time.Instant;

public record StorageDownloadUrl(
		String url,
		Instant expiresAt
) {
}
