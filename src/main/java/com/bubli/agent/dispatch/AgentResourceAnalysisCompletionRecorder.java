package com.bubli.agent.dispatch;

import com.bubli.resource.dto.ResourceAnalysisSource;
import com.bubli.resource.dto.PreparedResourceEmbeddingIndex;
import com.bubli.resource.service.ResourceAnalysisPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentResourceAnalysisCompletionRecorder {

	private final ResourceAnalysisPublicService resourceAnalysisService;
	private final AgentResourceAnalysisCompletionWriter completionWriter;

	public int complete(
			AgentJobQueueMessage message,
			ResourceAnalysisSource source,
			Map<String, Object> aiAnalysisJson,
			List<AgentJobExecutionSuggestionDraft> suggestionDrafts
	) {
		PreparedResourceEmbeddingIndex preparedIndex =
				resourceAnalysisService.prepareEmbeddingIndex(source);
		return completionWriter.complete(message, source, aiAnalysisJson, suggestionDrafts, preparedIndex);
	}
}
