package com.bubli.agent.service;

import com.bubli.agent.config.AgentRagProperties;
import com.bubli.agent.dto.ProjectRoomRagContext;
import com.bubli.agent.type.AgentCommandMode;
import com.bubli.resource.dto.ResourceSearchHit;
import com.bubli.resource.service.ResourceSemanticSearchPublicService;
import com.bubli.resource.type.ResourceSearchScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectRoomRagGroundingService {

	private final ResourceSemanticSearchPublicService resourceSemanticSearchService;
	private final AgentRagProperties agentRagProperties;

	@Transactional(readOnly = true)
	public ProjectRoomRagContext retrieve(UUID userId, UUID roomId, String query, String locale) {
		return retrieve(userId, roomId, query, locale, AgentCommandMode.ANSWER);
	}

	@Transactional(readOnly = true)
	public ProjectRoomRagContext retrieve(
			UUID userId,
			UUID roomId,
			String query,
			String locale,
			AgentCommandMode mode
	) {
		if (!agentRagProperties.enabled()) {
			return ProjectRoomRagContext.ungrounded();
		}
		try {
			List<ResourceSearchHit> hits = resourceSemanticSearchService.search(
					userId,
					ResourceSearchScope.ROOM_SHARED,
					roomId,
					query,
					agentRagProperties.topK(),
					AgentQuerySupport.documentQueryLanguage(query)
			);
			double minSimilarity = minSimilarity(mode);
			List<ResourceSearchHit> groundedHits = hits.stream()
					.filter(hit -> hit.similarityScore() >= minSimilarity)
					.toList();
			if (groundedHits.isEmpty()) {
				return ProjectRoomRagContext.ungrounded();
			}
			double maxSimilarity = groundedHits.stream()
					.map(ResourceSearchHit::similarityScore)
					.max(Comparator.naturalOrder())
					.orElse(0.0D);
			return new ProjectRoomRagContext(true, groundedHits, maxSimilarity, promptBlock(groundedHits));
		} catch (RuntimeException exception) {
			log.warn("Project room RAG retrieval failed. userId={}, roomId={}", userId, roomId, exception);
			return ProjectRoomRagContext.ungrounded();
		}
	}

	private double minSimilarity(AgentCommandMode mode) {
		if (mode == AgentCommandMode.SUGGEST) {
			return agentRagProperties.suggestMinSimilarity();
		}
		return agentRagProperties.minSimilarity();
	}

	private String promptBlock(List<ResourceSearchHit> hits) {
		StringBuilder prompt = new StringBuilder();
		for (ResourceSearchHit hit : hits) {
			prompt.append("[Source]\n")
					.append("resourceId=").append(hit.resourceId()).append('\n')
					.append("chunkIndex=").append(hit.chunkIndex()).append('\n')
					.append("pageNumber=").append(hit.pageNumber()).append('\n')
					.append("startLine=").append(hit.startLine()).append('\n')
					.append("endLine=").append(hit.endLine()).append('\n')
					.append("similarityScore=").append(hit.similarityScore()).append('\n')
					.append("chunkText=\n")
					.append(hit.chunkText()).append("\n\n");
		}
		return prompt.toString().trim();
	}
}
