package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentModelCallLog;
import com.bubli.agent.repository.AgentModelCallLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentJobExecutionModelCallLogRecorder {

	private final AgentModelCallLogRepository agentModelCallLogRepository;

	public int recordModelCallLogs(UUID jobId, List<AgentJobExecutionModelCallLog> modelCallLogs) {
		if (modelCallLogs == null || modelCallLogs.isEmpty()) {
			return 0;
		}
		for (AgentJobExecutionModelCallLog modelCallLog : modelCallLogs) {
			agentModelCallLogRepository.save(AgentModelCallLog.create(
					jobId,
					modelCallLog.promptVersion(),
					modelCallLog.schemaVersion(),
					modelCallLog.modelName(),
					modelCallLog.latencyMs(),
					modelCallLog.inputTokens(),
					modelCallLog.outputTokens(),
					modelCallLog.errorCode()
			));
		}
		return modelCallLogs.size();
	}
}
