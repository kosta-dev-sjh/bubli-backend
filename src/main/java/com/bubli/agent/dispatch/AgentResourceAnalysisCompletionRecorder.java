package com.bubli.agent.dispatch;

import com.bubli.agent.dto.CreateAgentSuggestionCommand;
import com.bubli.agent.service.AgentSuggestionService;
import com.bubli.resource.dto.ResourceAnalysisSource;
import com.bubli.resource.service.ResourceAnalysisPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentResourceAnalysisCompletionRecorder {

	private final ResourceAnalysisPublicService resourceAnalysisService;
	private final AgentSuggestionService agentSuggestionService;

	@Transactional
	public int complete(
			AgentJobQueueMessage message,
			ResourceAnalysisSource source,
			Map<String, Object> aiAnalysisJson,
			List<AgentJobExecutionSuggestionDraft> suggestionDrafts
	) {
		resourceAnalysisService.completeAnalysisForJob(source, message.jobId(), aiAnalysisJson);
		return recordSuggestions(message, suggestionDrafts);
	}

	private int recordSuggestions(AgentJobQueueMessage message, List<AgentJobExecutionSuggestionDraft> suggestionDrafts) {
		if (suggestionDrafts == null || suggestionDrafts.isEmpty()) {
			return 0;
		}
		for (AgentJobExecutionSuggestionDraft draft : suggestionDrafts) {
			agentSuggestionService.createDraft(new CreateAgentSuggestionCommand(
					message.requestedByUserId(),
					message.roomId(),
					message.jobId(),
					message.resourceId(),
					draft.suggestionType(),
					draft.payloadJson(),
					draft.evidenceJson()
			));
		}
		return suggestionDrafts.size();
	}
}
