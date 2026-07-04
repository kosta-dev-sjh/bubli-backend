package com.bubli.resource.controller;

import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.JwtTokenProvider;
import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.entity.RoomMember;
import com.bubli.project.repository.ProjectRoomRepository;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.ProjectRoomStatus;
import com.bubli.resource.repository.ResourceFileRepository;
import com.bubli.resource.repository.ResourceRepository;
import com.bubli.resource.repository.ResourceVersionRepository;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.storage.repository.StorageUsageRepository;
import com.bubli.storage.type.StorageScope;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
class DocumentControllerIntegrationTest extends PostgresIntegrationTestSupport {

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
	ResourceRepository resourceRepository;

	@Autowired
	ResourceFileRepository resourceFileRepository;

	@Autowired
	ResourceVersionRepository resourceVersionRepository;

	@Autowired
	StorageUsageRepository storageUsageRepository;

	@Autowired
	AgentJobRepository agentJobRepository;

	@Test
	void uploadContractDocumentPersistsResourceUsageAndAnalyzeJob() throws Exception {
		User user = createUser("google-sub-contract-document-upload", "민서");
		ProjectRoom room = projectRoomRepository.save(ProjectRoom.create(
				user.getId(),
				"계약 검토 프로젝트",
				null,
				null,
				null,
				null,
				null,
				ProjectRoomStatus.ACTIVE
		));
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), user.getId()));
		byte[] content = "프로젝트 요구사항 정리".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"requirements.txt",
				"text/plain",
				content
		);

		mockMvc.perform(multipart("/api/project-rooms/{roomId}/contract-documents", room.getId())
						.file(file)
						.param("documentType", "REQUIREMENT")
						.param("autoAnalyze", "true")
						.header(AUTHORIZATION, bearerToken(user.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.resourceId").isNotEmpty())
				.andExpect(jsonPath("$.data.jobId").isNotEmpty())
				.andExpect(jsonPath("$.data.status").value("PENDING"))
				.andExpect(jsonPath("$.data.autoAnalyze").value(true))
				.andExpect(jsonPath("$.error").value(nullValue()));

		var resource = resourceRepository.findAll().getFirst();
		assertThat(resource.getOwnerId()).isEqualTo(user.getId());
		assertThat(resource.getRoomId()).isEqualTo(room.getId());
		assertThat(resource.getStatus()).isEqualTo(ResourceStatus.ANALYZING);
		assertThat(resourceFileRepository.findByResourceId(resource.getId())).hasSize(1);
		assertThat(resourceVersionRepository.findMaxVersionNo(resource.getId())).isEqualTo(1);

		var usage = storageUsageRepository
				.findByRoomIdAndStorageScope(room.getId(), StorageScope.ROOM)
				.orElseThrow();
		assertThat(usage.getUsedBytes()).isEqualTo((long) content.length);

		var job = agentJobRepository.findAll().getFirst();
		assertThat(job.getRequestedByUserId()).isEqualTo(user.getId());
		assertThat(job.getRoomId()).isEqualTo(room.getId());
		assertThat(job.getResourceId()).isEqualTo(resource.getId());
		assertThat(job.getJobType()).isEqualTo(AgentJobType.ANALYZE_RESOURCE);
		assertThat(job.getStatus()).isEqualTo(AgentJobStatus.PENDING);
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

	private String bearerToken(java.util.UUID userId) {
		return "Bearer " + jwtTokenProvider.createAccessToken(new AuthUser(userId));
	}
}
