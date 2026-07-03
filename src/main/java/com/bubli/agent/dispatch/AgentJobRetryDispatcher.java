package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentJobRetryDispatcher {

	private final AgentJobRepository agentJobRepository;
	private final AgentJobDispatchPort agentJobDispatchPort;
	private final AgentJobDispatchFailureRecorder failureRecorder;
	private final AgentJobDispatchSuccessRecorder successRecorder;

	@Transactional
	public int dispatchRetryableFailedJobs(int maxRetryCount, int batchSize) {
		Pageable pageable = PageRequest.of(0, Math.max(1, batchSize));
		List<AgentJob> retryableJobs = agentJobRepository
				.findByStatusAndRetryCountLessThan(AgentJobStatus.FAILED, maxRetryCount, pageable)
				.getContent();

		int dispatchedCount = 0;
		for (AgentJob agentJob : retryableJobs) {
			AgentJobDispatchCommand command = AgentJobDispatchCommand.from(agentJob);
			log.info(
					"Dispatching retryable agent job. jobId={}, jobType={}, roomId={}, resourceId={}, retryCount={}, maxRetryCount={}, errorCode={}, errorMessage={}",
					agentJob.getId(),
					agentJob.getJobType(),
					agentJob.getRoomId(),
					agentJob.getResourceId(),
					agentJob.getRetryCount(),
					maxRetryCount,
					agentJob.getErrorCode(),
					truncate(agentJob.getErrorMessage())
			);
			try {
				agentJobDispatchPort.dispatch(command);
			} catch (RuntimeException exception) {
				log.warn(
						"Failed to dispatch retryable agent job. jobId={}, jobType={}, retryCount={}, maxRetryCount={}",
						agentJob.getId(),
						agentJob.getJobType(),
						agentJob.getRetryCount(),
						maxRetryCount,
						exception
				);
				failureRecorder.recordEnqueueFailure(command, exception);
				continue;
			}
			agentJob.markRetryQueued();
			log.info(
					"Retryable agent job queued. jobId={}, jobType={}, retryCount={}, status={}",
					agentJob.getId(),
					agentJob.getJobType(),
					agentJob.getRetryCount(),
					agentJob.getStatus()
			);
			try {
				successRecorder.recordQueued(command);
			} catch (RuntimeException exception) {
				log.warn("Failed to record retried agent job event. jobId={}", command.jobId(), exception);
			}
			dispatchedCount++;
		}
		return dispatchedCount;
	}

	private String truncate(String value) {
		if (value == null || value.length() <= 300) {
			return value;
		}
		return value.substring(0, 300) + "...";
	}
}
