package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.entity.AgentJobEvent;
import com.bubli.agent.repository.AgentJobEventRepository;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentJobExecutionResultRecorder {

	static final String SUCCEEDED_EVENT_TYPE = "SUCCEEDED";
	static final String FAILED_EVENT_TYPE = "FAILED";
	static final String SUCCEEDED_EVENT_MESSAGE = "에이전트 작업 실행을 완료했습니다.";
	static final String DEFAULT_FAILURE_MESSAGE = "에이전트 작업 실행에 실패했습니다.";

	private final AgentJobRepository agentJobRepository;
	private final AgentJobEventRepository agentJobEventRepository;

	@Transactional
	public boolean recordSucceeded(UUID jobId) {
		return agentJobRepository.findById(jobId)
				.filter(agentJob -> agentJob.getStatus() == AgentJobStatus.RUNNING)
				.map(this::markSucceeded)
				.orElse(false);
	}

	@Transactional
	public boolean recordFailed(UUID jobId, String errorCode, String errorMessage) {
		return agentJobRepository.findById(jobId)
				.filter(agentJob -> agentJob.getStatus() == AgentJobStatus.RUNNING)
				.map(agentJob -> markFailed(agentJob, errorCode, errorMessage))
				.orElse(false);
	}

	private boolean markSucceeded(AgentJob agentJob) {
		agentJob.markSucceeded();
		agentJobEventRepository.save(AgentJobEvent.create(
				agentJob.getId(),
				SUCCEEDED_EVENT_TYPE,
				SUCCEEDED_EVENT_MESSAGE
		));
		return true;
	}

	private boolean markFailed(AgentJob agentJob, String errorCode, String errorMessage) {
		String message = failureMessage(errorMessage);
		agentJob.markFailed(errorCode, message);
		agentJobEventRepository.save(AgentJobEvent.create(
				agentJob.getId(),
				FAILED_EVENT_TYPE,
				message
		));
		return true;
	}

	private String failureMessage(String errorMessage) {
		if (errorMessage == null || errorMessage.isBlank()) {
			return DEFAULT_FAILURE_MESSAGE;
		}
		return errorMessage;
	}
}
