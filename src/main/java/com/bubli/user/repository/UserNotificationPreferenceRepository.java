package com.bubli.user.repository;

import com.bubli.user.entity.UserNotificationPreference;
import com.bubli.user.entity.UserNotificationPreferenceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, UserNotificationPreferenceId> {

	List<UserNotificationPreference> findByIdUserId(UUID userId);

	@Modifying
	@Query(value = """
			INSERT INTO user_notification_preferences (
			    user_id, notification_type, enabled
			)
			VALUES (
			    :userId, :notificationType, :enabled
			)
			ON CONFLICT (user_id, notification_type)
			DO UPDATE SET enabled = EXCLUDED.enabled
			""", nativeQuery = true)
	int upsertEnabled(
			@Param("userId") UUID userId,
			@Param("notificationType") String notificationType,
			@Param("enabled") boolean enabled
	);
}
