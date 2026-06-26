package com.bubli.resource.controller;

import com.bubli.global.security.AuthUser;
import com.bubli.global.security.JwtTokenProvider;
import com.bubli.resource.entity.Resource;
import com.bubli.resource.repository.ResourceRepository;
import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.resource.type.ResourceVisibility;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
class ResourceControllerIntegrationTest extends PostgresIntegrationTestSupport {

	private static final String AUTHORIZATION = "Authorization";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JwtTokenProvider jwtTokenProvider;

	@Autowired
	UserRepository userRepository;

	@Autowired
	ResourceRepository resourceRepository;

	@BeforeEach
	void setUp() {
		resourceRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void uploadResourcePersistsResource() throws Exception {
		User user = createUser("google-sub-resource-upload", "정현");
		MockMultipartFile file = new MockMultipartFile(
				"file", "test.txt", "text/plain", "테스트 파일 내용".getBytes()
		);

		mockMvc.perform(multipart("/api/resources")
						.file(file)
						.param("title", "테스트 문서")
						.param("kind", "FILE")
						.param("visibility", "PERSONAL")
						.header(AUTHORIZATION, bearerToken(user.getId(), "junghyun@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").isNotEmpty())
				.andExpect(jsonPath("$.data.ownerId").value(user.getId().toString()))
				.andExpect(jsonPath("$.data.title").value("테스트 문서"))
				.andExpect(jsonPath("$.data.kind").value("FILE"))
				.andExpect(jsonPath("$.data.visibility").value("PERSONAL"))
				.andExpect(jsonPath("$.data.roomId").value(nullValue()))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(resourceRepository.findAll()).hasSize(1);
	}

	@Test
	void getPersonalResourcesReturnsOwnResources() throws Exception {
		User user = createUser("google-sub-resource-list", "미연");
		User other = createUser("google-sub-resource-list-other", "준화");
		saveResource(user.getId(), "내 메모", ResourceKind.MEMO, ResourceVisibility.PERSONAL);
		saveResource(other.getId(), "다른 사람 메모", ResourceKind.MEMO, ResourceVisibility.PERSONAL);

		mockMvc.perform(get("/api/resources")
						.header(AUTHORIZATION, bearerToken(user.getId(), "miyeon@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items", hasSize(1)))
				.andExpect(jsonPath("$.data.items[0].title").value("내 메모"))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	@Test
	void getResourceReturnsDetail() throws Exception {
		User user = createUser("google-sub-resource-detail", "재민");
		Resource resource = saveResource(user.getId(), "상세 조회 자료", ResourceKind.MEMO, ResourceVisibility.PERSONAL);

		mockMvc.perform(get("/api/resources/{resourceId}", resource.getId())
						.header(AUTHORIZATION, bearerToken(user.getId(), "jaemin@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(resource.getId().toString()))
				.andExpect(jsonPath("$.data.ownerId").value(user.getId().toString()))
				.andExpect(jsonPath("$.data.title").value("상세 조회 자료"))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	@Test
	void updateResourceChangesTitle() throws Exception {
		User user = createUser("google-sub-resource-update", "민서");
		Resource resource = saveResource(user.getId(), "기존 제목", ResourceKind.MEMO, ResourceVisibility.PERSONAL);

		mockMvc.perform(patch("/api/resources/{resourceId}", resource.getId())
						.header(AUTHORIZATION, bearerToken(user.getId(), "minseo@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "수정된 제목"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(resource.getId().toString()))
				.andExpect(jsonPath("$.data.title").value("수정된 제목"))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	@Test
	void deleteResourceMarksAsDeleted() throws Exception {
		User user = createUser("google-sub-resource-delete", "서현");
		Resource resource = saveResource(user.getId(), "삭제할 자료", ResourceKind.MEMO, ResourceVisibility.PERSONAL);

		mockMvc.perform(delete("/api/resources/{resourceId}", resource.getId())
						.header(AUTHORIZATION, bearerToken(user.getId(), "seohyeon@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(resourceRepository.findByIdAndDeletedAtIsNull(resource.getId())).isEmpty();
	}

	@Test
	void getResourceRejectsOtherUsersPersonalResource() throws Exception {
		User owner = createUser("google-sub-resource-owner", "지우");
		User other = createUser("google-sub-resource-outsider", "민준");
		Resource resource = saveResource(owner.getId(), "개인 자료", ResourceKind.MEMO, ResourceVisibility.PERSONAL);

		mockMvc.perform(get("/api/resources/{resourceId}", resource.getId())
						.header(AUTHORIZATION, bearerToken(other.getId(), "minjun@example.com")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error.code").value("RESOURCE_403_001"));
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

	private Resource saveResource(UUID ownerId, String title, ResourceKind kind, ResourceVisibility visibility) {
		return resourceRepository.save(Resource.create(ownerId, null, title, kind, visibility, ResourceStatus.READY));
	}

	private String bearerToken(UUID userId, String email) {
		return "Bearer " + jwtTokenProvider.createAccessToken(new AuthUser(userId, email));
	}
}
