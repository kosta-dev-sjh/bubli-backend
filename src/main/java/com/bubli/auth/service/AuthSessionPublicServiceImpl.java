package com.bubli.auth.service;

import com.bubli.auth.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthSessionPublicServiceImpl implements AuthSessionPublicService {

	private final UserSessionRepository userSessionRepository;

	@Override
	@Transactional
	public void revokeAllUserSessions(UUID userId) {
		userSessionRepository.findByUserId(userId)
				.forEach(session -> {
					if (session.isActiveAt(java.time.Instant.now())) {
						session.revoke();
					}
				});
	}
}
