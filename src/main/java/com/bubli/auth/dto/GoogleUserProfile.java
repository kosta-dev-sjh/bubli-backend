package com.bubli.auth.dto;

public record GoogleUserProfile(
		String sub,
		String name,
		String picture,
		String locale
) {
}
