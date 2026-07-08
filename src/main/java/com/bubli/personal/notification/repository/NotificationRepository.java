package com.bubli.personal.notification.repository;

import com.bubli.personal.notification.entity.Notification;
import com.bubli.personal.notification.type.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

	Page<Notification> findAllByUserIdAndStatus(UUID userId, NotificationStatus status, Pageable pageable);

	Page<Notification> findAllByUserId(UUID userId, Pageable pageable);

	Page<Notification> findAllByUserIdAndStatusNot(UUID userId, NotificationStatus status, Pageable pageable);

	long countByUserIdAndStatus(UUID userId, NotificationStatus status);

	Page<Notification> findAllByUserIdAndCreatedAtBetween(UUID userId, Instant from, Instant to, Pageable pageable);

	@Modifying
	@Query("""
			update Notification notification
			set notification.status = :readStatus,
			    notification.readAt = :now
			where notification.userId = :userId
			  and notification.status = :unreadStatus
			""")
	int markAllAsRead(
			@Param("userId") UUID userId,
			@Param("unreadStatus") NotificationStatus unreadStatus,
			@Param("readStatus") NotificationStatus readStatus,
			@Param("now") Instant now
	);
}
