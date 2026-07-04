package com.bubli.auth.repository;

import com.bubli.auth.entity.UserSession;
import com.bubli.auth.type.ClientType;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@Transactional
class UserSessionRepositoryIntegrationTest extends PostgresIntegrationTestSupport {

	@Autowired
	UserRepository userRepository;

	@Autowired
	UserSessionRepository userSessionRepository;

	@Test
	void upsertActiveSessionKeepsOneSessionPerUserAndClientType() {
		User user = userRepository.save(User.createGoogleUser(
				"google-sub-auth-session",
				"auth-session",
				"미연",
				null,
				"ko",
				"Asia/Seoul"
		));
		Instant firstExpiresAt = Instant.now().plusSeconds(3600);
		Instant secondExpiresAt = Instant.now().plusSeconds(7200);

		userSessionRepository.upsertActiveSession(
				UUID.randomUUID(),
				user.getId(),
				"refresh-hash-1",
				ClientType.TAURI.name(),
				firstExpiresAt
		);
		userSessionRepository.upsertActiveSession(
				UUID.randomUUID(),
				user.getId(),
				"refresh-hash-2",
				ClientType.TAURI.name(),
				secondExpiresAt
		);

		assertThat(userSessionRepository.findByUserId(user.getId())).hasSize(1);
		assertThat(userSessionRepository.findByRefreshTokenAndClientType("refresh-hash-1", ClientType.TAURI))
				.isEmpty();
		UserSession session = userSessionRepository
				.findByRefreshTokenAndClientTypeForUpdate("refresh-hash-2", ClientType.TAURI)
				.orElseThrow();
		assertThat(session.getUserId()).isEqualTo(user.getId());
		assertThat(session.getClientType()).isEqualTo(ClientType.TAURI);
		assertThat(session.isActiveAt(Instant.now())).isTrue();
		assertThat(session.getRevokedAt()).isNull();
	}
}
