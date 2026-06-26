package com.bubli.user.repository;

import com.bubli.user.entity.UserNotificationPreference;
import com.bubli.user.entity.UserNotificationPreferenceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, UserNotificationPreferenceId> {

	List<UserNotificationPreference> findByIdUserId(UUID userId);
}
