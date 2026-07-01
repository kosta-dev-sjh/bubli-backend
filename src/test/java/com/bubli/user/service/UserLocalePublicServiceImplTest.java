package com.bubli.user.service;

import com.bubli.global.security.AuthUser;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserLocalePublicServiceImplTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final UserLocalePublicServiceImpl service = new UserLocalePublicServiceImpl(userRepository);

	@Test
	void userLocaleWinsOverAcceptLanguage() {
		UUID userId = UUID.randomUUID();
		User user = User.createGoogleUser("google-sub", "bubli", "Milo", null, "ja", "Asia/Tokyo");
		ReflectionTestUtils.setField(user, "id", userId);
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));

		String locale = service.resolveLocaleCode(new AuthUser(userId), "en-US,en;q=0.9");

		assertThat(locale).isEqualTo("ja-JP");
	}

	@Test
	void acceptLanguageIsUsedWhenUserIsMissing() {
		UUID userId = UUID.randomUUID();
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		String locale = service.resolveLocaleCode(userId, "en-US,en;q=0.9");

		assertThat(locale).isEqualTo("en-US");
	}

	@Test
	void acceptLanguageSkipsUnsupportedLanguages() {
		String locale = service.resolveLocaleCode("fr-FR,ja-JP;q=0.8,en-US;q=0.7");

		assertThat(locale).isEqualTo("ja-JP");
	}

	@Test
	void invalidOrMissingAcceptLanguageFallsBackToKorean() {
		assertThat(service.resolveLocaleCode((String) null)).isEqualTo("ko-KR");
		assertThat(service.resolveLocaleCode("not a valid language header!")).isEqualTo("ko-KR");
	}
}
