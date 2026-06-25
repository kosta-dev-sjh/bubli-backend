package com.bubli.agent.dispatch;

import com.bubli.agent.dto.CreateAgentSuggestionCommand;
import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.service.AgentSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentJobExecutionSuggestionRecorder {

	private final AgentSuggestionService agentSuggestionService;

	public int recordSuggestions(AgentJob agentJob, List<AgentJobExecutionSuggestionDraft> suggestionDrafts) {
		if (suggestionDrafts == null || suggestionDrafts.isEmpty()) {
			return 0;
		}
		for (AgentJobExecutionSuggestionDraft draft : suggestionDrafts) {
			agentSuggestionService.createDraft(new CreateAgentSuggestionCommand(
					agentJob.getRequestedByUserId(),
					agentJob.getRoomId(),
					agentJob.getId(),
					agentJob.getResourceId(),
					draft.suggestionType(),
					draft.payloadJson(),
					draft.evidenceJson()
			));
		}
		return suggestionDrafts.size();
	}
}
