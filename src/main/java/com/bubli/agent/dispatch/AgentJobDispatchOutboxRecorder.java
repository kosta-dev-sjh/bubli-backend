package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentDispatchOutbox;
import com.bubli.agent.repository.AgentDispatchOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentJobDispatchOutboxRecorder {

	private final AgentDispatchOutboxRepository agentDispatchOutboxRepository;
	private final ObjectMapper objectMapper;

	@Transactional
	public void recordPending(AgentJobDispatchCommand command) {
		agentDispatchOutboxRepository.save(AgentDispatchOutbox.pending(
				command.jobId(),
				payloadJson(command)
		));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordDispatched(UUID jobId) {
		agentDispatchOutboxRepository.findByJobId(jobId)
				.ifPresent(AgentDispatchOutbox::markDispatched);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordFailure(UUID jobId, String errorCode, String errorMessage) {
		agentDispatchOutboxRepository.findByJobId(jobId)
				.ifPresent(outbox -> outbox.markFailed(errorCode, errorMessage));
	}

	private String payloadJson(AgentJobDispatchCommand command) {
		try {
			return objectMapper.writeValueAsString(command);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize agent dispatch outbox payload.", exception);
		}
	}
}
