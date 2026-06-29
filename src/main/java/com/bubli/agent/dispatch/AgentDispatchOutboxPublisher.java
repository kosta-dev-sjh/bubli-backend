package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentDispatchOutbox;
import com.bubli.agent.repository.AgentDispatchOutboxRepository;
import com.bubli.agent.type.AgentDispatchOutboxStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AgentDispatchOutboxPublisher {

	static final String RETRY_FAILURE_ERROR_CODE = "AGENT_DISPATCH_OUTBOX_RETRY_FAILED";
	static final String DEAD_LETTER_ERROR_CODE = "AGENT_DISPATCH_OUTBOX_DEAD_LETTER";
	static final String DEAD_LETTER_MESSAGE = "에이전트 dispatch outbox 재시도 한도를 초과했습니다.";

	private final AgentDispatchOutboxRepository agentDispatchOutboxRepository;
	private final AgentJobDispatchPort agentJobDispatchPort;
	private final ObjectMapper objectMapper;

	@Transactional
	public int publishPending(int batchSize) {
		return publish(AgentDispatchOutboxStatus.PENDING, batchSize, Integer.MAX_VALUE);
	}

	@Transactional
	public int retryFailed(int batchSize, int maxRetryCount) {
		return publish(AgentDispatchOutboxStatus.FAILED, batchSize, maxRetryCount);
	}

	private int publish(AgentDispatchOutboxStatus status, int batchSize, int maxRetryCount) {
		if (batchSize <= 0) {
			return 0;
		}
		int dispatchedCount = 0;
		for (AgentDispatchOutbox outbox : agentDispatchOutboxRepository
				.findByStatus(status, PageRequest.of(0, batchSize, Sort.by("createdAt").ascending()))
				.getContent()) {
			if (outbox.getRetryCount() >= maxRetryCount) {
				outbox.markDeadLetter(DEAD_LETTER_ERROR_CODE, DEAD_LETTER_MESSAGE);
				continue;
			}
			if (dispatch(outbox)) {
				dispatchedCount++;
			}
		}
		return dispatchedCount;
	}

	private boolean dispatch(AgentDispatchOutbox outbox) {
		try {
			agentJobDispatchPort.dispatch(toCommand(outbox));
			outbox.markDispatched();
			return true;
		} catch (RuntimeException exception) {
			outbox.markFailed(RETRY_FAILURE_ERROR_CODE, errorMessage(exception));
			return false;
		}
	}

	private AgentJobDispatchCommand toCommand(AgentDispatchOutbox outbox) {
		try {
			return objectMapper.readValue(outbox.getPayloadJson(), AgentJobDispatchCommand.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to deserialize agent dispatch outbox payload.", exception);
		}
	}

	private String errorMessage(RuntimeException exception) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			return exception.getClass().getSimpleName();
		}
		return message;
	}
}
