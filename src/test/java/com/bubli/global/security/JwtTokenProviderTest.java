package com.bubli.global.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    @Test
    void createAccessTokenAndParseAuthUser() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "secret", "test-secret-key-must-be-at-least-32-bytes");
        ReflectionTestUtils.setField(tokenProvider, "accessTokenExpireMs", 3_600_000L);
        ReflectionTestUtils.setField(tokenProvider, "refreshTokenExpireMs", 1_209_600_000L);
        tokenProvider.init();

        AuthUser authUser = new AuthUser(UUID.randomUUID(), "user@example.com");
        String token = tokenProvider.createAccessToken(authUser);

        AuthUser parsed = tokenProvider.getAuthUser(token);

        assertThat(parsed.userId()).isEqualTo(authUser.userId());
        assertThat(parsed.email()).isEqualTo(authUser.email());
    }
}
