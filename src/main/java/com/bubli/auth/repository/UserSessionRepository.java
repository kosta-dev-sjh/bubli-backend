package com.bubli.auth.repository;

import com.bubli.auth.entity.UserSession;
import com.bubli.auth.type.ClientType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
	Optional<UserSession> findByUserIdAndClientType(UUID userId, ClientType clientType);

	Optional<UserSession> findByRefreshTokenAndClientType(String refreshTokenHash, ClientType clientType);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select session
			from UserSession session
			where session.refreshToken = :refreshTokenHash
			  and session.clientType = :clientType
			""")
	Optional<UserSession> findByRefreshTokenAndClientTypeForUpdate(
			@Param("refreshTokenHash") String refreshTokenHash,
			@Param("clientType") ClientType clientType
	);

	List<UserSession> findByUserId(UUID userId);

	@Modifying
	@Query(value = """
			INSERT INTO user_sessions (
			    id,
			    user_id,
			    refresh_token,
			    client_type,
			    status,
			    expires_at,
			    last_used_at,
			    created_at,
			    updated_at
			)
			VALUES (
			    :id,
			    :userId,
			    :refreshTokenHash,
			    :clientType,
			    'ACTIVE',
			    :expiresAt,
			    CURRENT_TIMESTAMP,
			    CURRENT_TIMESTAMP,
			    CURRENT_TIMESTAMP
			)
			ON CONFLICT (user_id, client_type)
			DO UPDATE SET
			    refresh_token = EXCLUDED.refresh_token,
			    status = 'ACTIVE',
			    expires_at = EXCLUDED.expires_at,
			    last_used_at = CURRENT_TIMESTAMP,
			    revoked_at = NULL,
			    updated_at = CURRENT_TIMESTAMP
			""", nativeQuery = true)
	int upsertActiveSession(
			@Param("id") UUID id,
			@Param("userId") UUID userId,
			@Param("refreshTokenHash") String refreshTokenHash,
			@Param("clientType") String clientType,
			@Param("expiresAt") Instant expiresAt
	);
}
