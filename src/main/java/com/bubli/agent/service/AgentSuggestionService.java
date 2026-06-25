package com.bubli.agent.service;

import com.bubli.agent.dto.AgentSuggestionResult;
import com.bubli.agent.dto.CreateAgentSuggestionCommand;
import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.repository.AgentSuggestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AgentSuggestionService {

	private final AgentSuggestionRepository agentSuggestionRepository;

	@Transactional
	public AgentSuggestionResult createDraft(CreateAgentSuggestionCommand command) {
		AgentSuggestion suggestion = AgentSuggestion.createDraft(
				command.userId(),
				command.roomId(),
				command.jobId(),
				command.resourceId(),
				command.suggestionType(),
				command.payloadJson(),
				command.evidenceJson()
		);
		return AgentSuggestionResult.from(agentSuggestionRepository.save(suggestion));
	}
}
