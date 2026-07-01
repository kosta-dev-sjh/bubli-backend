package com.bubli.global.locale;

import java.util.Arrays;
import java.util.Locale;

public enum SupportedLocale {
	KO_KR("ko-KR", "ko"),
	EN_US("en-US", "en"),
	JA_JP("ja-JP", "ja");

	public static final SupportedLocale DEFAULT = KO_KR;

	private final String code;
	private final String language;

	SupportedLocale(String code, String language) {
		this.code = code;
		this.language = language;
	}

	public String code() {
		return code;
	}

	public Locale toJavaLocale() {
		return Locale.forLanguageTag(code);
	}

	public static String normalize(String value) {
		return resolve(value).code();
	}

	public static SupportedLocale resolve(String value) {
		if (value == null || value.isBlank()) {
			return DEFAULT;
		}
		String normalized = value.trim().replace('_', '-');
		return Arrays.stream(values())
				.filter(locale -> locale.code.equalsIgnoreCase(normalized)
						|| locale.language.equalsIgnoreCase(normalized))
				.findFirst()
				.orElse(DEFAULT);
	}
}
