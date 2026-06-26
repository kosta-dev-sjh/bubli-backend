package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentModelCallLog;
import com.bubli.agent.repository.AgentModelCallLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AgentJobExecutionModelCallLogRecorderTest {

	@Test
	void recordModelCallLogsStoresLogsForJob() {
		AgentModelCallLogRepository repository = mock(AgentModelCallLogRepository.class);
		AgentJobExecutionModelCallLogRecorder recorder = new AgentJobExecutionModelCallLogRecorder(repository);
		UUID jobId = UUID.randomUUID();
		AgentJobExecutionModelCallLog modelCallLog = new AgentJobExecutionModelCallLog(
				"analyze-resource-v1",
				"agent-output-v1",
				"gpt-test",
				1500L,
				120,
				80,
				null
		);

		int recordedCount = recorder.recordModelCallLogs(jobId, List.of(modelCallLog));

		assertThat(recordedCount).isEqualTo(1);
		ArgumentCaptor<AgentModelCallLog> logCaptor = ArgumentCaptor.forClass(AgentModelCallLog.class);
		verify(repository).save(logCaptor.capture());
		AgentModelCallLog savedLog = logCaptor.getValue();
		assertThat(savedLog.getJobId()).isEqualTo(jobId);
		assertThat(savedLog.getPromptVersion()).isEqualTo("analyze-resource-v1");
		assertThat(savedLog.getSchemaVersion()).isEqualTo("agent-output-v1");
		assertThat(savedLog.getModelName()).isEqualTo("gpt-test");
		assertThat(savedLog.getLatencyMs()).isEqualTo(1500L);
		assertThat(savedLog.getInputTokens()).isEqualTo(120);
		assertThat(savedLog.getOutputTokens()).isEqualTo(80);
		assertThat(savedLog.getErrorCode()).isNull();
	}

	@Test
	void recordModelCallLogsReturnsZeroWhenLogsAreEmpty() {
		AgentModelCallLogRepository repository = mock(AgentModelCallLogRepository.class);
		AgentJobExecutionModelCallLogRecorder recorder = new AgentJobExecutionModelCallLogRecorder(repository);

		int recordedCount = recorder.recordModelCallLogs(UUID.randomUUID(), List.of());

		assertThat(recordedCount).isZero();
		verifyNoInteractions(repository);
	}
}
