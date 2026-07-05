package com.bubli.user.repository;

import com.bubli.user.entity.UserPreference;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {

	Optional<UserPreference> findByUserId(UUID userId);

	@Modifying
	@Query(value = """
			INSERT INTO user_preferences (
			    id, user_id, created_at, updated_at
			)
			VALUES (
			    :id, :userId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
			)
			ON CONFLICT (user_id) DO NOTHING
			""", nativeQuery = true)
	int insertDefaultIfAbsent(
			@Param("id") UUID id,
			@Param("userId") UUID userId
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select preference
			from UserPreference preference
			where preference.userId = :userId
			""")
	Optional<UserPreference> findByUserIdForUpdate(@Param("userId") UUID userId);
}
