package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentJobEvent;
import com.bubli.agent.repository.AgentJobEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AgentJobDispatchSuccessRecorder {

	static final String QUEUED_EVENT_TYPE = "QUEUED";
	static final String QUEUED_EVENT_MESSAGE = "에이전트 작업을 실행 대기열에 등록했습니다.";

	private final AgentJobEventRepository agentJobEventRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordQueued(AgentJobDispatchCommand command) {
		agentJobEventRepository.save(
				AgentJobEvent.create(command.jobId(), QUEUED_EVENT_TYPE, QUEUED_EVENT_MESSAGE)
		);
	}
}
