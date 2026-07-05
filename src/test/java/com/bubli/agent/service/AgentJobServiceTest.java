package com.bubli.agent.service;

import com.bubli.agent.dispatch.AgentJobDispatchOutboxRecorder;
import com.bubli.agent.dto.CreateAgentJobCommand;
import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobEventRepository;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobType;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.user.service.UserLocalePublicService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentJobServiceTest {

	@Test
	void createAddsResolvedUserLocaleWhenPayloadDoesNotContainLocale() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobEventRepository agentJobEventRepository = mock(AgentJobEventRepository.class);
		ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
		AgentJobDispatchOutboxRecorder dispatchOutboxRecorder = mock(AgentJobDispatchOutboxRecorder.class);
		ProjectMembershipPublicService projectMembershipPublicService = mock(ProjectMembershipPublicService.class);
		UserLocalePublicService userLocalePublicService = mock(UserLocalePublicService.class);
		AgentJobService service = new AgentJobService(
				agentJobRepository,
				agentJobEventRepository,
				eventPublisher,
				dispatchOutboxRecorder,
				projectMembershipPublicService,
				userLocalePublicService
		);
		when(userLocalePublicService.resolveLocaleCode(eq(userId), isNull())).thenReturn("ja-JP");
		when(agentJobRepository.save(any(AgentJob.class))).thenAnswer(invocation -> {
			AgentJob job = invocation.getArgument(0);
			ReflectionTestUtils.setField(job, "id", UUID.randomUUID());
			return job;
		});

		service.create(userId, new CreateAgentJobCommand(
				roomId,
				resourceId,
				AgentJobType.ANALYZE_RESOURCE,
				Map.of("source", "public-service")
		));

		verify(agentJobRepository).save(org.mockito.ArgumentMatchers.assertArg(job ->
				assertThat(job.getRequestPayload())
						.containsEntry("source", "public-service")
						.containsEntry("locale", "ja-JP")
		));
		verify(dispatchOutboxRecorder).recordPending(org.mockito.ArgumentMatchers.assertArg(command ->
				assertThat(command.requestPayload())
						.containsEntry("source", "public-service")
						.containsEntry("locale", "ja-JP")
		));
	}

	@Test
	void createNormalizesExplicitPayloadLocale() {
		UUID userId = UUID.randomUUID();
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobService service = new AgentJobService(
				agentJobRepository,
				mock(AgentJobEventRepository.class),
				mock(ApplicationEventPublisher.class),
				mock(AgentJobDispatchOutboxRecorder.class),
				mock(ProjectMembershipPublicService.class),
				mock(UserLocalePublicService.class)
		);
		when(agentJobRepository.save(any(AgentJob.class))).thenAnswer(invocation -> {
			AgentJob job = invocation.getArgument(0);
			ReflectionTestUtils.setField(job, "id", UUID.randomUUID());
			return job;
		});

		service.create(userId, new CreateAgentJobCommand(
				null,
				null,
				AgentJobType.GENERATE_TASKS,
				Map.of("locale", "ja")
		));

		verify(agentJobRepository).save(org.mockito.ArgumentMatchers.assertArg(job ->
				assertThat(job.getRequestPayload()).containsEntry("locale", "ja-JP")
		));
	}
}
