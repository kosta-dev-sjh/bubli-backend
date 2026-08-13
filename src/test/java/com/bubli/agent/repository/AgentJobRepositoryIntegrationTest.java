package com.bubli.agent.repository;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.type.AgentJobType;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentJobRepositoryIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    AgentJobRepository agentJobRepository;

    @Autowired
    UserRepository userRepository;

    @Test
    void compositeIdempotencyKeyAllowsSameKeyForDifferentUsers() {
        User firstUser = createUser("google-sub-agent-idempotency-first");
        User secondUser = createUser("google-sub-agent-idempotency-second");

        AgentJob first = agentJobRepository.saveAndFlush(job(
                firstUser.getId(),
                AgentJobType.GENERATE_TASKS,
                "shared-client-key"
        ));
        AgentJob second = agentJobRepository.saveAndFlush(job(
                secondUser.getId(),
                AgentJobType.GENERATE_TASKS,
                "shared-client-key"
        ));

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void compositeIdempotencyKeyAllowsSameKeyForDifferentJobTypes() {
        User user = createUser("google-sub-agent-idempotency-job-types");

        AgentJob first = agentJobRepository.saveAndFlush(job(
                user.getId(),
                AgentJobType.GENERATE_TASKS,
                "same-key"
        ));
        AgentJob second = agentJobRepository.saveAndFlush(job(
                user.getId(),
                AgentJobType.GENERATE_WBS,
                "same-key"
        ));

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void compositeIdempotencyKeyRejectsSameUserJobTypeAndKey() {
        User user = createUser("google-sub-agent-idempotency-duplicate");
        agentJobRepository.saveAndFlush(job(user.getId(), AgentJobType.GENERATE_TASKS, "duplicate-key"));

        assertThatThrownBy(() -> agentJobRepository.saveAndFlush(job(
                user.getId(),
                AgentJobType.GENERATE_TASKS,
                "duplicate-key"
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void acquiresIdempotencyScopeLockInsideTransaction() {
        int result = agentJobRepository.acquireIdempotencyScopeLock("user:job-type:key");

        assertThat(result).isEqualTo(1);
    }

    private AgentJob job(UUID userId, AgentJobType jobType, String idempotencyKey) {
        return AgentJob.create(
                userId,
                null,
                null,
                jobType,
                Map.of("idempotencyKey", idempotencyKey, "locale", "ko-KR"),
                idempotencyKey
        );
    }

    private User createUser(String googleSub) {
        return userRepository.save(User.createGoogleUser(
                googleSub,
                googleSub.replace("google-sub-", ""),
                "테스트 사용자",
                null,
                "ko",
                "Asia/Seoul"
        ));
    }
}
