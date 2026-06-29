package com.bubli.personal.notification.controller;

import com.bubli.global.security.AuthUser;
import com.bubli.global.security.JwtTokenProvider;
import com.bubli.personal.notification.repository.NotificationRepository;
import com.bubli.personal.notification.type.NotificationStatus;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
class NotificationControllerIntegrationTest extends PostgresIntegrationTestSupport {

	private static final String AUTHORIZATION = "Authorization";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JwtTokenProvider jwtTokenProvider;

	@Autowired
	UserRepository userRepository;

	@Autowired
	NotificationRepository notificationRepository;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		notificationRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void getNotificationsReturnsOwnNotifications() throws Exception {
		User user = createUser("google-sub-notif-list", "정현");
		UUID notifId = saveNotification(user.getId(), "새 메시지가 도착했습니다");

		mockMvc.perform(get("/api/notifications")
						.header(AUTHORIZATION, bearerToken(user.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items", hasSize(1)))
				.andExpect(jsonPath("$.data.items[0].id").value(notifId.toString()))
				.andExpect(jsonPath("$.data.items[0].title").value("새 메시지가 도착했습니다"))
				.andExpect(jsonPath("$.data.items[0].status").value("UNREAD"))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	@Test
	void readNotificationMarksAsRead() throws Exception {
		User user = createUser("google-sub-notif-read", "미연");
		UUID notifId = saveNotification(user.getId(), "읽음 처리 테스트");

		mockMvc.perform(patch("/api/notifications/{id}/read", notifId)
						.header(AUTHORIZATION, bearerToken(user.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(notificationRepository.findById(notifId).orElseThrow().getStatus())
				.isEqualTo(NotificationStatus.READ);
	}

	@Test
	void archiveNotificationMarksAsArchived() throws Exception {
		User user = createUser("google-sub-notif-archive", "준화");
		UUID notifId = saveNotification(user.getId(), "보관 처리 테스트");

		mockMvc.perform(patch("/api/notifications/{id}/archive", notifId)
						.header(AUTHORIZATION, bearerToken(user.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(notificationRepository.findById(notifId).orElseThrow().getStatus())
				.isEqualTo(NotificationStatus.ARCHIVED);
	}

	@Test
	void readNotificationRejectsDifferentUsersNotification() throws Exception {
		User owner = createUser("google-sub-notif-owner", "재민");
		User other = createUser("google-sub-notif-other", "민서");
		UUID notifId = saveNotification(owner.getId(), "다른 사람 알림");

		mockMvc.perform(patch("/api/notifications/{id}/read", notifId)
						.header(AUTHORIZATION, bearerToken(other.getId())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error.code").value("NOTIFICATION_403_001"));
	}

	private User createUser(String googleSub, String name) {
		return userRepository.save(User.createGoogleUser(
				googleSub,
				googleSub.replace("google-sub-", ""),
				name,
				null,
				"ko",
				"Asia/Seoul"
		));
	}

	private UUID saveNotification(UUID userId, String title) {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update(
				"INSERT INTO notifications (id, user_id, source_type, title, status, created_at) VALUES (?, ?, ?, ?, ?, ?)",
				id, userId, "MESSAGE", title, "UNREAD", Timestamp.from(Instant.now())
		);
		return id;
	}

	private String bearerToken(UUID userId) {
		return "Bearer " + jwtTokenProvider.createAccessToken(new AuthUser(userId));
	}
}
