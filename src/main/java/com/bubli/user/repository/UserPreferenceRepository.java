package com.bubli.user.repository;

import com.bubli.user.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {

	Optional<UserPreference> findByUserId(UUID userId);
}
