package com.bubli.user.repository;

import com.bubli.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByGoogleSub(String googleSub);

	boolean existsByBubliId(String bubliId);

	Optional<User> findByBubliId(String bubliId);
}
