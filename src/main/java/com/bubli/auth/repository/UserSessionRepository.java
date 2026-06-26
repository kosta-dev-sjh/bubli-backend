package com.bubli.auth.repository;

import com.bubli.auth.entity.UserSession;
import com.bubli.auth.type.ClientType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
	Optional<UserSession> findByUserIdAndClientType(UUID userId, ClientType clientType);

	Optional<UserSession> findByRefreshTokenAndClientType(String refreshTokenHash, ClientType clientType);

	List<UserSession> findByUserId(UUID userId);
}
