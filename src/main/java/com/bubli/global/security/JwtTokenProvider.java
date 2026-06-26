package com.bubli.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expire-ms}")
    private long accessTokenExpireMs;

    @Value("${jwt.refresh-token-expire-ms}")
    private long refreshTokenExpireMs;

    private SecretKey secretKey;

    @PostConstruct
    void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(AuthUser authUser) {
        return createToken(authUser, accessTokenExpireMs);
    }

    public String createRefreshToken(AuthUser authUser) {
        return createToken(authUser, refreshTokenExpireMs);
    }

    public AuthUser getAuthUser(String token) {
        Claims claims = parseClaims(token);
        return new AuthUser(
                UUID.fromString(claims.getSubject())
        );
    }

    public long getAccessTokenExpireMs() {
        return accessTokenExpireMs;
    }

    public long getRefreshTokenExpireMs() {
        return refreshTokenExpireMs;
    }

    private String createToken(AuthUser authUser, long expireMs) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(expireMs);

        return Jwts.builder()
                .subject(authUser.userId().toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
