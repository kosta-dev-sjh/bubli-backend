package com.bubli.global.locale;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SupportedLocaleTest {

	@Test
	void normalizesSupportedLocaleAliases() {
		assertThat(SupportedLocale.normalize("ko")).isEqualTo("ko-KR");
		assertThat(SupportedLocale.normalize("ko_KR")).isEqualTo("ko-KR");
		assertThat(SupportedLocale.normalize("en")).isEqualTo("en-US");
		assertThat(SupportedLocale.normalize("en-US")).isEqualTo("en-US");
		assertThat(SupportedLocale.normalize("ja")).isEqualTo("ja-JP");
		assertThat(SupportedLocale.normalize("ja_JP")).isEqualTo("ja-JP");
	}

	@Test
	void fallsBackToKoreanForMissingOrUnsupportedLocale() {
		assertThat(SupportedLocale.normalize(null)).isEqualTo("ko-KR");
		assertThat(SupportedLocale.normalize(" ")).isEqualTo("ko-KR");
		assertThat(SupportedLocale.normalize("fr-FR")).isEqualTo("ko-KR");
	}
}
