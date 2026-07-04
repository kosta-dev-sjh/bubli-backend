package com.bubli.voice.controller;

import com.bubli.global.security.AuthUser;
import com.bubli.global.security.JwtTokenProvider;
import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.entity.RoomMember;
import com.bubli.project.repository.ProjectRoomRepository;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.PaymentStatus;
import com.bubli.project.type.ProjectRoomStatus;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import com.bubli.voice.entity.VoiceParticipant;
import com.bubli.voice.entity.VoiceRoom;
import com.bubli.voice.repository.VoiceParticipantRepository;
import com.bubli.voice.repository.VoiceRoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
class VoiceRoomControllerIntegrationTest extends PostgresIntegrationTestSupport {

	private static final String AUTHORIZATION = "Authorization";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JwtTokenProvider jwtTokenProvider;

	@Autowired
	UserRepository userRepository;

	@Autowired
	ProjectRoomRepository projectRoomRepository;

	@Autowired
	RoomMemberRepository roomMemberRepository;

	@Autowired
	VoiceRoomRepository voiceRoomRepository;

	@Autowired
	VoiceParticipantRepository voiceParticipantRepository;

	@Test
	void getOpenVoiceRoomByProjectRoomReturnsParticipantsWithMicStatus() throws Exception {
		User requester = createUser("google-sub-voice-requester", "미연");
		User other = createUser("google-sub-voice-other", "수진");
		ProjectRoom projectRoom = projectRoomRepository.save(ProjectRoom.create(
				requester.getId(),
				"보이스 테스트룸",
				null,
				null,
				PaymentStatus.NOT_RECORDED,
				null,
				null,
				ProjectRoomStatus.ACTIVE
		));
		roomMemberRepository.save(RoomMember.createLeader(projectRoom.getId(), requester.getId()));
		roomMemberRepository.save(RoomMember.createMember(projectRoom.getId(), other.getId()));
		VoiceRoom voiceRoom = voiceRoomRepository.save(VoiceRoom.create(projectRoom.getId(), requester.getId()));
		voiceParticipantRepository.save(VoiceParticipant.join(voiceRoom.getId(), requester.getId()));
		VoiceParticipant otherParticipant = VoiceParticipant.join(voiceRoom.getId(), other.getId());
		otherParticipant.updateMicStatus("MUTED");
		voiceParticipantRepository.save(otherParticipant);

		mockMvc.perform(get("/api/voice/rooms")
						.param("roomId", projectRoom.getId().toString())
						.header(AUTHORIZATION, bearerToken(requester.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(voiceRoom.getId().toString()))
				.andExpect(jsonPath("$.data.roomId").value(projectRoom.getId().toString()))
				.andExpect(jsonPath("$.data.participants", hasSize(2)))
				.andExpect(jsonPath("$.data.participants[*].userName", containsInAnyOrder("미연", "수진")))
				.andExpect(jsonPath("$.data.participants[*].micStatus", containsInAnyOrder("UNMUTED", "MUTED")))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	private User createUser(String googleSub, String name) {
		String bubliId = "bubli-" + UUID.randomUUID().toString().substring(0, 8);
		return userRepository.save(User.createGoogleUser(googleSub, bubliId, name, null, "ko-KR", "Asia/Seoul"));
	}

	private String bearerToken(UUID userId) {
		return "Bearer " + jwtTokenProvider.createAccessToken(new AuthUser(userId));
	}
}
