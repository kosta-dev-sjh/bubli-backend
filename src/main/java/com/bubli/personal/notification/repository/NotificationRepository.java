package com.bubli.personal.notification.repository;

import com.bubli.personal.notification.entity.Notification;
import com.bubli.personal.notification.type.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

	Page<Notification> findAllByUserIdAndStatus(UUID userId, NotificationStatus status, Pageable pageable);

	Page<Notification> findAllByUserId(UUID userId, Pageable pageable);

	long countByUserIdAndStatus(UUID userId, NotificationStatus status);
}
