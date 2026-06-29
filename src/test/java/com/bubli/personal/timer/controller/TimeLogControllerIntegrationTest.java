package com.bubli.personal.timer.controller;

import com.bubli.global.security.AuthUser;
import com.bubli.global.security.JwtTokenProvider;
import com.bubli.personal.timer.entity.TimeLog;
import com.bubli.personal.timer.repository.TimeLogRepository;
import com.bubli.personal.timer.type.TimeLogStatus;
import com.bubli.personal.timer.type.TimerType;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
class TimeLogControllerIntegrationTest extends PostgresIntegrationTestSupport {

	private static final String AUTHORIZATION = "Authorization";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JwtTokenProvider jwtTokenProvider;

	@Autowired
	UserRepository userRepository;

	@Autowired
	TimeLogRepository timeLogRepository;

	@BeforeEach
	void setUp() {
		timeLogRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void startTimeLogCreatesRunningTimeLog() throws Exception {
		User user = createUser("google-sub-timelog-start", "정현");

		mockMvc.perform(post("/api/time-logs/start")
						.header(AUTHORIZATION, bearerToken(user.getId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "timerType": "GENERAL",
								  "idempotencyKey": "key-start-001"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").isNotEmpty())
				.andExpect(jsonPath("$.data.userId").value(user.getId().toString()))
				.andExpect(jsonPath("$.data.status").value("RUNNING"))
				.andExpect(jsonPath("$.data.timerType").value("GENERAL"))
				.andExpect(jsonPath("$.data.idempotencyKey").value("key-start-001"))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(timeLogRepository.findAll()).hasSize(1);
		assertThat(timeLogRepository.findAll().getFirst().getStatus()).isEqualTo(TimeLogStatus.RUNNING);
	}

	@Test
	void pauseTimeLogChangesStatusToPaused() throws Exception {
		User user = createUser("google-sub-timelog-pause", "미연");
		UUID timeLogId = startTimeLog(user, "key-pause-001");

		mockMvc.perform(patch("/api/time-logs/{timeLogId}/pause", timeLogId)
						.header(AUTHORIZATION, bearerToken(user.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(timeLogId.toString()))
				.andExpect(jsonPath("$.data.status").value("PAUSED"))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(timeLogRepository.findById(timeLogId).orElseThrow().getStatus())
				.isEqualTo(TimeLogStatus.PAUSED);
	}

	@Test
	void resumeTimeLogChangesStatusToRunning() throws Exception {
		User user = createUser("google-sub-timelog-resume", "준화");
		UUID timeLogId = startTimeLog(user, "key-resume-001");

		mockMvc.perform(patch("/api/time-logs/{timeLogId}/pause", timeLogId)
						.header(AUTHORIZATION, bearerToken(user.getId())))
				.andExpect(status().isOk());

		mockMvc.perform(patch("/api/time-logs/{timeLogId}/resume", timeLogId)
						.header(AUTHORIZATION, bearerToken(user.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(timeLogId.toString()))
				.andExpect(jsonPath("$.data.status").value("RUNNING"))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	@Test
	void stopTimeLogChangesStatusToEnded() throws Exception {
		User user = createUser("google-sub-timelog-stop", "재민");
		UUID timeLogId = startTimeLog(user, "key-stop-001");

		mockMvc.perform(patch("/api/time-logs/{timeLogId}/stop", timeLogId)
						.header(AUTHORIZATION, bearerToken(user.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(timeLogId.toString()))
				.andExpect(jsonPath("$.data.status").value("ENDED"))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(timeLogRepository.findById(timeLogId).orElseThrow().getStatus())
				.isEqualTo(TimeLogStatus.ENDED);
	}

	@Test
	void startTimeLogRejectsWhenAlreadyRunning() throws Exception {
		User user = createUser("google-sub-timelog-conflict", "민서");
		startTimeLog(user, "key-conflict-001");

		mockMvc.perform(post("/api/time-logs/start")
						.header(AUTHORIZATION, bearerToken(user.getId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "timerType": "GENERAL",
								  "idempotencyKey": "key-conflict-002"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error.code").value("PERSONAL_409_001"));
	}

	@Test
	void pauseTimeLogRejectsOtherUsersTimeLog() throws Exception {
		User owner = createUser("google-sub-timelog-owner", "서현");
		User other = createUser("google-sub-timelog-intruder", "지우");
		UUID timeLogId = startTimeLog(owner, "key-owner-001");

		mockMvc.perform(patch("/api/time-logs/{timeLogId}/pause", timeLogId)
						.header(AUTHORIZATION, bearerToken(other.getId())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error.code").value("PERSONAL_404_001"));
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

	private UUID startTimeLog(User user, String idempotencyKey) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/time-logs/start")
						.header(AUTHORIZATION, bearerToken(user.getId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "timerType": "GENERAL",
								  "idempotencyKey": "%s"
								}
								""".formatted(idempotencyKey)))
				.andExpect(status().isOk())
				.andReturn();
		String body = result.getResponse().getContentAsString();
		return UUID.fromString(body.replaceAll("(?s).*\"id\":\"([^\"]+)\".*", "$1"));
	}

	private String bearerToken(UUID userId) {
		return "Bearer " + jwtTokenProvider.createAccessToken(new AuthUser(userId));
	}
}
