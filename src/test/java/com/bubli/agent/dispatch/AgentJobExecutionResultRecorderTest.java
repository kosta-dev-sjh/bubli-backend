package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.entity.AgentJobEvent;
import com.bubli.agent.repository.AgentJobEventRepository;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
import com.bubli.personal.notification.service.NotificationPublicService;
import com.bubli.personal.notification.type.NotificationSourceType;
import com.bubli.project.service.ProjectRoomEventPublicService;
import com.bubli.user.service.UserLocalePublicService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentJobExecutionResultRecorderTest {

	@Test
	void recordSucceededMarksRunningJobSucceededAndStoresEvent() {
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobEventRepository agentJobEventRepository = mock(AgentJobEventRepository.class);
		AgentJobExecutionResultRecorder recorder = recorder(agentJobRepository, agentJobEventRepository);
		UUID jobId = UUID.randomUUID();
		AgentJob agentJob = runningJob(jobId);
		when(agentJobRepository.findById(jobId)).thenReturn(Optional.of(agentJob));

		boolean recorded = recorder.recordSucceeded(jobId);

		assertThat(recorded).isTrue();
		assertThat(agentJob.getStatus()).isEqualTo(AgentJobStatus.SUCCEEDED);
		assertThat(agentJob.getFinishedAt()).isNotNull();
		ArgumentCaptor<AgentJobEvent> eventCaptor = ArgumentCaptor.forClass(AgentJobEvent.class);
		verify(agentJobEventRepository).save(eventCaptor.capture());
		assertThat(eventCaptor.getValue().getJobId()).isEqualTo(jobId);
		assertThat(eventCaptor.getValue().getEventType())
				.isEqualTo(AgentJobExecutionResultRecorder.SUCCEEDED_EVENT_TYPE);
		assertThat(eventCaptor.getValue().getMessage())
				.isEqualTo(AgentJobExecutionResultRecorder.SUCCEEDED_EVENT_MESSAGE);
	}

	@Test
	void recordFailedMarksRunningJobFailedAndStoresEventWithoutRetryCountIncrement() {
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobEventRepository agentJobEventRepository = mock(AgentJobEventRepository.class);
		AgentJobExecutionResultRecorder recorder = recorder(agentJobRepository, agentJobEventRepository);
		UUID jobId = UUID.randomUUID();
		AgentJob agentJob = runningJob(jobId);
		when(agentJobRepository.findById(jobId)).thenReturn(Optional.of(agentJob));

		boolean recorded = recorder.recordFailed(jobId, "MODEL_TIMEOUT", "model timeout");

		assertThat(recorded).isTrue();
		assertThat(agentJob.getStatus()).isEqualTo(AgentJobStatus.FAILED);
		assertThat(agentJob.getErrorCode()).isEqualTo("MODEL_TIMEOUT");
		assertThat(agentJob.getErrorMessage()).isEqualTo("model timeout");
		assertThat(agentJob.getRetryCount()).isZero();
		assertThat(agentJob.getFinishedAt()).isNotNull();
		ArgumentCaptor<AgentJobEvent> eventCaptor = ArgumentCaptor.forClass(AgentJobEvent.class);
		verify(agentJobEventRepository).save(eventCaptor.capture());
		assertThat(eventCaptor.getValue().getJobId()).isEqualTo(jobId);
		assertThat(eventCaptor.getValue().getEventType())
				.isEqualTo(AgentJobExecutionResultRecorder.FAILED_EVENT_TYPE);
		assertThat(eventCaptor.getValue().getMessage()).isEqualTo("model timeout");
	}

	@Test
	void recordFailedUsesDefaultMessageWhenMessageIsBlank() {
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobEventRepository agentJobEventRepository = mock(AgentJobEventRepository.class);
		AgentJobExecutionResultRecorder recorder = recorder(agentJobRepository, agentJobEventRepository);
		UUID jobId = UUID.randomUUID();
		AgentJob agentJob = runningJob(jobId);
		when(agentJobRepository.findById(jobId)).thenReturn(Optional.of(agentJob));

		boolean recorded = recorder.recordFailed(jobId, "MODEL_ERROR", " ");

		assertThat(recorded).isTrue();
		assertThat(agentJob.getErrorMessage())
				.isEqualTo(AgentJobExecutionResultRecorder.DEFAULT_FAILURE_MESSAGE);
		ArgumentCaptor<AgentJobEvent> eventCaptor = ArgumentCaptor.forClass(AgentJobEvent.class);
		verify(agentJobEventRepository).save(eventCaptor.capture());
		assertThat(eventCaptor.getValue().getMessage())
				.isEqualTo(AgentJobExecutionResultRecorder.DEFAULT_FAILURE_MESSAGE);
	}

	@Test
	void recordSucceededLocalizesNotificationTitleAndBody() {
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobEventRepository agentJobEventRepository = mock(AgentJobEventRepository.class);
		NotificationPublicService notificationPublicService = mock(NotificationPublicService.class);
		ProjectRoomEventPublicService projectRoomEventPublicService = mock(ProjectRoomEventPublicService.class);
		UserLocalePublicService localeService = mock(UserLocalePublicService.class);
		when(localeService.resolveLocaleCode(any(UUID.class), any())).thenReturn("en-US");
		StaticMessageSource messageSource = new StaticMessageSource();
		messageSource.addMessage("agent.job.succeeded.event", Locale.US, "Agent job execution completed.");
		messageSource.addMessage("agent.job.succeeded.notification.title", Locale.US, "AI job completed.");
		messageSource.addMessage("agent.job.notification.body", Locale.US, "type={0}; id={1}; message={2}");
		AgentJobExecutionResultRecorder recorder = new AgentJobExecutionResultRecorder(
				agentJobRepository,
				agentJobEventRepository,
				notificationPublicService,
				projectRoomEventPublicService,
				messageSource,
				localeService
		);
		UUID jobId = UUID.randomUUID();
		AgentJob agentJob = runningJob(jobId);
		when(agentJobRepository.findById(jobId)).thenReturn(Optional.of(agentJob));

		recorder.recordSucceeded(jobId);

		verify(notificationPublicService).create(
				eq(agentJob.getRequestedByUserId()),
				eq(NotificationSourceType.AGENT),
				eq(jobId),
				eq("AI job completed."),
				org.mockito.ArgumentMatchers.contains("Agent job execution completed.")
		);
	}

	@Test
	void recordSucceededPublishesProjectRoomEventWhenJobBelongsToRoom() {
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobEventRepository agentJobEventRepository = mock(AgentJobEventRepository.class);
		NotificationPublicService notificationPublicService = mock(NotificationPublicService.class);
		ProjectRoomEventPublicService projectRoomEventPublicService = mock(ProjectRoomEventPublicService.class);
		UserLocalePublicService localeService = mock(UserLocalePublicService.class);
		when(localeService.resolveLocaleCode(any(UUID.class), any())).thenReturn("ko-KR");
		AgentJobExecutionResultRecorder recorder = new AgentJobExecutionResultRecorder(
				agentJobRepository,
				agentJobEventRepository,
				notificationPublicService,
				projectRoomEventPublicService,
				new StaticMessageSource(),
				localeService
		);
		UUID jobId = UUID.randomUUID();
		AgentJob agentJob = runningJob(jobId);
		when(agentJobRepository.findById(jobId)).thenReturn(Optional.of(agentJob));

		recorder.recordSucceeded(jobId);

		verify(projectRoomEventPublicService).recordAgentJobCompleted(
				eq(agentJob.getRequestedByUserId()),
				eq(agentJob.getRoomId()),
				eq(jobId),
				eq(agentJob.getJobType().name()),
				eq("SUCCEEDED"),
				eq(AgentJobExecutionResultRecorder.SUCCEEDED_EVENT_MESSAGE)
		);
	}

	@Test
	void recordSucceededIgnoresPendingJob() {
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobEventRepository agentJobEventRepository = mock(AgentJobEventRepository.class);
		AgentJobExecutionResultRecorder recorder = recorder(agentJobRepository, agentJobEventRepository);
		UUID jobId = UUID.randomUUID();
		AgentJob agentJob = AgentJob.create(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE
		);
		ReflectionTestUtils.setField(agentJob, "id", jobId);
		when(agentJobRepository.findById(jobId)).thenReturn(Optional.of(agentJob));

		boolean recorded = recorder.recordSucceeded(jobId);

		assertThat(recorded).isFalse();
		assertThat(agentJob.getStatus()).isEqualTo(AgentJobStatus.PENDING);
		verify(agentJobEventRepository, never()).save(any());
	}

	@Test
	void recordSucceededIgnoresMissingJob() {
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobEventRepository agentJobEventRepository = mock(AgentJobEventRepository.class);
		AgentJobExecutionResultRecorder recorder = recorder(agentJobRepository, agentJobEventRepository);
		UUID jobId = UUID.randomUUID();
		when(agentJobRepository.findById(jobId)).thenReturn(Optional.empty());

		boolean recorded = recorder.recordSucceeded(jobId);

		assertThat(recorded).isFalse();
		verify(agentJobEventRepository, never()).save(any());
	}

	private AgentJob runningJob(UUID jobId) {
		AgentJob agentJob = AgentJob.create(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE
		);
		ReflectionTestUtils.setField(agentJob, "id", jobId);
		agentJob.markRunning();
		return agentJob;
	}

	private AgentJobExecutionResultRecorder recorder(
			AgentJobRepository agentJobRepository,
			AgentJobEventRepository agentJobEventRepository
	) {
		UserLocalePublicService localeService = mock(UserLocalePublicService.class);
		when(localeService.resolveLocaleCode(any(UUID.class), any())).thenReturn("ko-KR");
		return new AgentJobExecutionResultRecorder(
				agentJobRepository,
				agentJobEventRepository,
				mock(NotificationPublicService.class),
				mock(ProjectRoomEventPublicService.class),
				new StaticMessageSource(),
				localeService
		);
	}
}
