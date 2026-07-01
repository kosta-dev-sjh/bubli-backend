package com.bubli.user.service;

import com.bubli.global.locale.SupportedLocale;
import com.bubli.global.security.AuthUser;
import com.bubli.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserLocalePublicServiceImpl implements UserLocalePublicService {

	private final UserRepository userRepository;

	@Override
	@Transactional(readOnly = true)
	public String resolveLocaleCode(AuthUser authUser, String acceptLanguageHeader) {
		return resolveLocaleCode(authUser == null ? null : authUser.userId(), acceptLanguageHeader);
	}

	@Override
	@Transactional(readOnly = true)
	public String resolveLocaleCode(UUID userId, String acceptLanguageHeader) {
		if (userId != null) {
			return userRepository.findById(userId)
					.map(user -> SupportedLocale.normalize(user.getLocale()))
					.orElseGet(() -> resolveLocaleCode(acceptLanguageHeader));
		}
		return resolveLocaleCode(acceptLanguageHeader);
	}

	@Override
	public String resolveLocaleCode(String acceptLanguageHeader) {
		return resolveAcceptLanguage(acceptLanguageHeader).code();
	}

	private SupportedLocale resolveAcceptLanguage(String acceptLanguageHeader) {
		if (acceptLanguageHeader == null || acceptLanguageHeader.isBlank()) {
			return SupportedLocale.DEFAULT;
		}
		try {
			for (Locale.LanguageRange range : Locale.LanguageRange.parse(acceptLanguageHeader)) {
				SupportedLocale resolved = SupportedLocale.resolve(range.getRange());
				if (resolved != SupportedLocale.DEFAULT || isKorean(range.getRange())) {
					return resolved;
				}
			}
		} catch (IllegalArgumentException ignored) {
			return SupportedLocale.DEFAULT;
		}
		return SupportedLocale.DEFAULT;
	}

	private boolean isKorean(String languageRange) {
		return languageRange != null && languageRange.toLowerCase(Locale.ROOT).startsWith("ko");
	}
}
