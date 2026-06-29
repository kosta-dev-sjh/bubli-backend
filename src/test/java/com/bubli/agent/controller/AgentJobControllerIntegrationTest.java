package com.bubli.agent.controller;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.entity.AgentJobEvent;
import com.bubli.agent.repository.AgentJobEventRepository;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobType;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.JwtTokenProvider;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
class AgentJobControllerIntegrationTest extends PostgresIntegrationTestSupport {

    private static final String AUTHORIZATION = "Authorization";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AgentJobRepository agentJobRepository;

    @Autowired
    AgentJobEventRepository agentJobEventRepository;

    @BeforeEach
    void setUp() {
        agentJobEventRepository.deleteAll();
        agentJobRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getJobEventsReturnsRequestedUsersJobEvents() throws Exception {
        User user = createUser("google-sub-agent-job-events", "미연");
        AgentJob job = agentJobRepository.save(AgentJob.create(
                user.getId(),
                null,
                null,
                AgentJobType.ANALYZE_RESOURCE
        ));
        AgentJobEvent event = agentJobEventRepository.save(AgentJobEvent.create(
                job.getId(),
                "QUEUED",
                "분석 작업이 대기열에 등록되었습니다."
        ));

        mockMvc.perform(get("/api/agent-jobs/{jobId}/events", job.getId())
                        .header(AUTHORIZATION, bearerToken(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].id").value(event.getId().toString()))
                .andExpect(jsonPath("$.data.items[0].jobId").value(job.getId().toString()))
                .andExpect(jsonPath("$.data.items[0].eventType").value("QUEUED"))
                .andExpect(jsonPath("$.data.items[0].message").value("분석 작업이 대기열에 등록되었습니다."))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    void getJobEventsHidesOtherUsersJob() throws Exception {
        User owner = createUser("google-sub-agent-job-owner", "정현");
        User other = createUser("google-sub-agent-job-other", "준화");
        AgentJob job = agentJobRepository.save(AgentJob.create(
                owner.getId(),
                null,
                null,
                AgentJobType.ANALYZE_RESOURCE
        ));
        agentJobEventRepository.save(AgentJobEvent.create(job.getId(), "QUEUED", "대기 중"));

        mockMvc.perform(get("/api/agent-jobs/{jobId}/events", job.getId())
                        .header(AUTHORIZATION, bearerToken(other.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("AGENT_404_001"));
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

    private String bearerToken(UUID userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(new AuthUser(userId));
    }
}
