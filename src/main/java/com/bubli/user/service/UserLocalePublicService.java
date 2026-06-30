package com.bubli.user.service;

import com.bubli.global.security.AuthUser;

import java.util.UUID;

public interface UserLocalePublicService {

	String resolveLocaleCode(AuthUser authUser, String acceptLanguageHeader);

	String resolveLocaleCode(UUID userId, String acceptLanguageHeader);

	String resolveLocaleCode(String acceptLanguageHeader);
}
