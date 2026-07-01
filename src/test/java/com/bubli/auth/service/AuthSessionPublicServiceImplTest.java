package com.bubli.auth.service;

import com.bubli.auth.entity.UserSession;
import com.bubli.auth.repository.UserSessionRepository;
import com.bubli.auth.type.ClientType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class AuthSessionPublicServiceImplTest {

	@Test
	void revokeAllUserSessionsRevokesActiveSessions() {
		UserSessionRepository repository = mock(UserSessionRepository.class);
		AuthSessionPublicServiceImpl service = new AuthSessionPublicServiceImpl(repository);
		UUID userId = UUID.randomUUID();
		UserSession webSession = UserSession.create(userId, "web-token", ClientType.WEB, Instant.now().plusSeconds(60));
		UserSession tauriSession = UserSession.create(userId, "tauri-token", ClientType.TAURI, Instant.now().plusSeconds(60));
		given(repository.findByUserId(userId)).willReturn(List.of(webSession, tauriSession));

		service.revokeAllUserSessions(userId);

		assertThat(webSession.isActiveAt(Instant.now())).isFalse();
		assertThat(tauriSession.isActiveAt(Instant.now())).isFalse();
	}
}
