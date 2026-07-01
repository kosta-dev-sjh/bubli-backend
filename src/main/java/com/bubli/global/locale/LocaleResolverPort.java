package com.bubli.global.locale;

import com.bubli.global.security.AuthUser;

import java.util.UUID;

public interface LocaleResolverPort {

    String resolveLocaleCode(AuthUser authUser, String acceptLanguageHeader);

    String resolveLocaleCode(UUID userId, String acceptLanguageHeader);

    String resolveLocaleCode(String acceptLanguageHeader);
}
