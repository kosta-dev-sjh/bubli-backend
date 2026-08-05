package com.bubli.agent.service;

import com.bubli.agent.config.AgentRagProperties;
import com.bubli.agent.dto.AgentSuggestionResponse;
import com.bubli.agent.dto.ProjectRoomGroundingContext;
import com.bubli.agent.dto.ProjectRoomGroundingEvidence;
import com.bubli.agent.dto.ProjectRoomGroundingSourceType;
import com.bubli.agent.type.AgentCommandMode;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.dto.ResourceSearchHit;
import com.bubli.resource.dto.ResourceSummaryResult;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.resource.service.ResourceSearchMetricsPublicService;
import com.bubli.resource.service.ResourceSemanticSearchPublicService;
import com.bubli.resource.type.ResourceSearchScope;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.service.SchedulePublicService;
import com.bubli.work.task.dto.TaskResult;
import com.bubli.work.task.service.TaskPublicService;
import com.bubli.work.task.type.TaskStatus;
import com.bubli.work.wbs.dto.WbsItemResult;
import com.bubli.work.wbs.service.WbsItemPublicService;
import com.bubli.work.wbs.type.WbsStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectRoomGroundingService {

	private static final int DEFAULT_CONTEXT_LIMIT = 10;
	private static final int RESOURCE_TITLE_MATCH_MIN_SCORE = 3;
	private static final int STRONG_RESOURCE_TITLE_MATCH_SCORE = 8;
	private static final int TITLE_SCOPED_SEARCH_TOP_K_MULTIPLIER = 3;
	private static final double PRECISE_GROUNDING_RELAXED_MIN_SIMILARITY = 0.62D;
	private static final double TITLE_SCOPED_RELAXED_MIN_SIMILARITY = 0.55D;
	private static final double TITLE_SCOPED_KEYWORD_MIN_SCORE = 0.35D;
	private static final Duration SCHEDULE_LOOKBACK = Duration.ofDays(7);
	private static final Duration SCHEDULE_LOOKAHEAD = Duration.ofDays(30);

	private final ResourceSemanticSearchPublicService resourceSemanticSearchService;
	private final ResourcePublicService resourcePublicService;
	private final AgentRagProperties agentRagProperties;
	private final TaskPublicService taskPublicService;
	private final WbsItemPublicService wbsItemPublicService;
	private final SchedulePublicService schedulePublicService;
	private final AgentSuggestionPublicService agentSuggestionPublicService;
	private final ResourceSearchMetricsPublicService resourceSearchMetrics;
	private final ProjectRoomDocumentFusionService documentFusionService;

	@Transactional(readOnly = true)
	public ProjectRoomGroundingContext retrieve(
			UUID userId,
			UUID roomId,
			String message,
			String locale,
			AgentCommandMode mode
	) {
		try {
			EnumSet<ProjectRoomGroundingSourceType> requestedSources = requestedSources(message, mode);
			if (requestedSources.isEmpty()) {
				return ProjectRoomGroundingContext.ungrounded();
			}

			AgentSearchQueryAnalysis queryAnalysis = AgentQuerySupport.analyze(message, locale);
			boolean requireSemanticDocumentEvidence = requestedSources.contains(ProjectRoomGroundingSourceType.DOCUMENT)
					&& mode == AgentCommandMode.ANSWER
					&& AgentQuerySupport.requiresSemanticDocumentEvidence(message);
			boolean documentOverviewRequest = AgentQuerySupport.isDocumentOverviewRequest(message);
			AgentQuerySupport.WorkStateIntent workStateIntent = AgentQuerySupport.workStateIntent(message);
			List<String> retrievalFailures = new ArrayList<>();
			List<ResourceTitleMatch> titleMatches = retrieveDocumentTitleMatches(
					userId,
					roomId,
					message,
					requestedSources,
					List.of()
			);
			List<ProjectRoomDocumentCandidate> documentCandidates = new ArrayList<>();
			documentCandidates.addAll(toCandidates(retrieveDocumentHits(
					userId,
					roomId,
					queryAnalysis.normalizedQuery(),
					mode,
					requestedSources,
					requireSemanticDocumentEvidence,
					retrievalFailures
			), "SEMANTIC", queryAnalysis, false));
			documentCandidates.addAll(toCandidates(retrieveKeywordDocumentHits(
					userId,
					roomId,
					queryAnalysis,
					requestedSources,
					retrievalFailures
			), "KEYWORD", queryAnalysis, false));
			documentCandidates.addAll(toCandidates(retrieveTitleScopedDocumentHits(
					userId,
					roomId,
					queryAnalysis.normalizedQuery(),
					mode,
					requestedSources,
					titleMatches,
					retrievalFailures
			), "TITLE_SCOPED_SEMANTIC", queryAnalysis, true));
			documentCandidates.addAll(toCandidates(retrieveTitleScopedKeywordDocumentHits(
					userId,
					roomId,
					queryAnalysis,
					requestedSources,
					titleMatches,
					retrievalFailures
			), "TITLE_SCOPED_KEYWORD", queryAnalysis, true));
			documentCandidates = preferTitleMatchedDocumentCandidates(documentCandidates, titleMatches);
			List<ProjectRoomDocumentCandidate> representativeDocumentCandidates = toCandidates(retrieveRepresentativeDocumentChunks(
					userId,
					roomId,
					requestedSources,
					requireSemanticDocumentEvidence,
					documentOverviewRequest,
					titleMatches,
					retrievalFailures
			), "REPRESENTATIVE", queryAnalysis, true);
			ProjectRoomDocumentFusionService.ProjectRoomDocumentFusionResult fusionResult = documentFusionService.fuse(
					queryAnalysis,
					documentCandidates,
					agentRagProperties.topK(),
					mode == AgentCommandMode.SUGGEST
							? ProjectRoomDocumentFusionService.AgentCommandModeValue.SUGGEST
							: ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
			);
			if (!fusionResult.grounded() && !representativeDocumentCandidates.isEmpty()) {
				fusionResult = documentFusionService.fuse(
						queryAnalysis,
						representativeDocumentCandidates,
						agentRagProperties.topK(),
						mode == AgentCommandMode.SUGGEST
								? ProjectRoomDocumentFusionService.AgentCommandModeValue.SUGGEST
								: ProjectRoomDocumentFusionService.AgentCommandModeValue.ANSWER
				);
			}
			List<ResourceSearchHit> ragHits = fusionResult.hits();
			Map<UUID, ProjectRoomDocumentCandidate> selectedCandidatesByEmbeddingId =
					candidatesByEmbeddingId(fusionResult.selected());
			Map<UUID, String> ragResourceTitles = resourceTitles(
					userId,
					ragHits.stream()
							.map(ResourceSearchHit::resourceId)
							.distinct()
							.toList()
			);
			ragHits = titleResolvedDocumentHits(ragHits, ragResourceTitles);
			titleMatches = excludeTitleMatchesAlreadyCoveredByRag(titleMatches, ragHits);
			titleMatches = titleMatches.stream()
					.filter(this::hasUsableTitleMatchEvidence)
					.toList();
			List<ProjectRoomGroundingEvidence> evidenceItems = new ArrayList<>();
			StringBuilder prompt = new StringBuilder();

			appendDocumentEvidence(ragHits, selectedCandidatesByEmbeddingId, ragResourceTitles, evidenceItems, prompt);
			if (!requireSemanticDocumentEvidence) {
				appendResourceTitleEvidence(titleMatches, evidenceItems, prompt);
				appendRecentResourceSummaryEvidence(userId, roomId, requestedSources, evidenceItems, prompt);
			}
			appendTaskEvidence(roomId, requestedSources, workStateIntent, evidenceItems, prompt);
			appendWbsEvidence(roomId, requestedSources, workStateIntent, evidenceItems, prompt);
			appendScheduleEvidence(roomId, requestedSources, evidenceItems, prompt);
			appendAgentSuggestionEvidence(userId, roomId, requestedSources, evidenceItems, prompt);

			if (evidenceItems.isEmpty()) {
				if (hasRetrievalFailureForRequestedSources(requestedSources, retrievalFailures)) {
					return ProjectRoomGroundingContext.retrievalFailed(String.join(",", retrievalFailures));
				}
				return ProjectRoomGroundingContext.ungrounded();
			}
			return new ProjectRoomGroundingContext(
					true,
					ragHits,
					maxSimilarity(ragHits),
					evidenceItems,
					prompt.toString().trim()
			);
		} catch (RuntimeException exception) {
			log.warn("Project room grounding retrieval failed. userId={}, roomId={}", userId, roomId, exception);
			return ProjectRoomGroundingContext.retrievalFailed("GROUNDING_RETRIEVAL_FAILED");
		}
	}

	private boolean hasRetrievalFailureForRequestedSources(
			EnumSet<ProjectRoomGroundingSourceType> requestedSources,
			List<String> retrievalFailures
	) {
		return requestedSources.contains(ProjectRoomGroundingSourceType.DOCUMENT) && !retrievalFailures.isEmpty();
	}

	private EnumSet<ProjectRoomGroundingSourceType> requestedSources(String message, AgentCommandMode mode) {
		EnumSet<ProjectRoomGroundingSourceType> sources = EnumSet.noneOf(ProjectRoomGroundingSourceType.class);
		if (AgentQuerySupport.isDocumentSourceRequest(message)) {
			sources.add(ProjectRoomGroundingSourceType.DOCUMENT);
		}
		if (isTaskSourceRequest(message, mode)) {
			sources.add(ProjectRoomGroundingSourceType.TASK);
		}
		if (isWbsSourceRequest(message, mode)) {
			sources.add(ProjectRoomGroundingSourceType.WBS);
		}
		if (AgentQuerySupport.isScheduleSourceRequest(message)) {
			sources.add(ProjectRoomGroundingSourceType.SCHEDULE);
		}
		if (AgentQuerySupport.isAgentSuggestionSourceRequest(message)) {
			sources.add(ProjectRoomGroundingSourceType.AGENT_SUGGESTION);
		}
		return sources;
	}

	private boolean isTaskSourceRequest(String message, AgentCommandMode mode) {
		return AgentQuerySupport.isTaskSourceRequest(message)
				&& (mode == AgentCommandMode.ANSWER || AgentQuerySupport.hasSourceIntent(message));
	}

	private boolean isWbsSourceRequest(String message, AgentCommandMode mode) {
		return AgentQuerySupport.isWbsSourceRequest(message)
				&& (mode == AgentCommandMode.ANSWER || AgentQuerySupport.hasSourceIntent(message));
	}

	private List<ResourceSearchHit> retrieveDocumentHits(
			UUID userId,
			UUID roomId,
			String searchQuery,
			AgentCommandMode mode,
			EnumSet<ProjectRoomGroundingSourceType> requestedSources,
			boolean requireSemanticDocumentEvidence,
			List<String> retrievalFailures
	) {
		if (!requestedSources.contains(ProjectRoomGroundingSourceType.DOCUMENT) || !agentRagProperties.enabled()) {
			return List.of();
		}
		List<ResourceSearchHit> hits;
		try {
			hits = resourceSemanticSearchService.search(
					userId,
					ResourceSearchScope.ROOM_SHARED,
					roomId,
					searchQuery,
					agentRagProperties.topK()
			);
		} catch (RuntimeException exception) {
			log.warn("Project room semantic document retrieval failed. userId={}, roomId={}", userId, roomId, exception);
			retrievalFailures.add("SEMANTIC_DOCUMENT_RETRIEVAL_FAILED");
			return List.of();
		}
		List<ResourceSearchHit> safeHits = hits == null ? List.of() : hits;
		List<ResourceSearchHit> acceptedHits = safeHits.stream()
				.filter(hit -> hit.similarityScore() >= documentMinSimilarity(mode, requireSemanticDocumentEvidence))
				.toList();
		resourceSearchMetrics.recordSelection("semantic", "room", safeHits.size(), acceptedHits.size());
		return acceptedHits;
	}

	private List<ResourceSearchHit> retrieveTitleScopedDocumentHits(
			UUID userId,
			UUID roomId,
			String searchQuery,
			AgentCommandMode mode,
			EnumSet<ProjectRoomGroundingSourceType> requestedSources,
			List<ResourceTitleMatch> titleMatches,
			List<String> retrievalFailures
	) {
		if (!requestedSources.contains(ProjectRoomGroundingSourceType.DOCUMENT)
				|| !agentRagProperties.enabled()
				|| titleMatches.isEmpty()) {
			return List.of();
		}
		List<UUID> resourceIds = titleMatches.stream()
				.map(match -> match.resource().id())
				.distinct()
				.toList();
		try {
			List<ResourceSearchHit> candidates = resourceSemanticSearchService.searchRoomSharedResources(
					userId,
					roomId,
					resourceIds,
					searchQuery,
					titleScopedTopK(resourceIds.size())
			);
			List<ResourceSearchHit> safeCandidates = candidates == null ? List.of() : candidates;
			List<ResourceSearchHit> acceptedHits = safeCandidates.stream()
					.filter(hit -> hit.similarityScore() >= titleScopedMinSimilarity(mode))
					.toList();
			resourceSearchMetrics.recordSelection(
					"semantic",
					"room_resources",
					safeCandidates.size(),
					acceptedHits.size()
			);
			return acceptedHits;
		} catch (RuntimeException exception) {
			log.warn("Project room title-scoped semantic document retrieval failed. userId={}, roomId={}",
					userId, roomId, exception);
			retrievalFailures.add("TITLE_SCOPED_SEMANTIC_DOCUMENT_RETRIEVAL_FAILED");
			return List.of();
		}
	}

	private List<ResourceSearchHit> retrieveKeywordDocumentHits(
			UUID userId,
			UUID roomId,
			AgentSearchQueryAnalysis queryAnalysis,
			EnumSet<ProjectRoomGroundingSourceType> requestedSources,
			List<String> retrievalFailures
	) {
		if (!requestedSources.contains(ProjectRoomGroundingSourceType.DOCUMENT)) {
			return List.of();
		}
		List<String> keywords = queryAnalysis.keywords();
		if (keywords.isEmpty()) {
			return List.of();
		}
		try {
			List<ResourceSearchHit> candidates = resourceSemanticSearchService.searchRoomSharedKeywords(
					userId,
					roomId,
					keywords,
					agentRagProperties.topK()
			);
			List<ResourceSearchHit> safeCandidates = candidates == null ? List.of() : candidates;
			List<ResourceSearchHit> acceptedHits = safeCandidates.stream()
					.filter(hit -> hit.similarityScore() >= TITLE_SCOPED_KEYWORD_MIN_SCORE)
					.toList();
			resourceSearchMetrics.recordSelection("keyword", "room", safeCandidates.size(), acceptedHits.size());
			return acceptedHits;
		} catch (RuntimeException exception) {
			log.warn("Project room keyword document retrieval failed. userId={}, roomId={}",
					userId, roomId, exception);
			retrievalFailures.add("KEYWORD_DOCUMENT_RETRIEVAL_FAILED");
			return List.of();
		}
	}

	private List<ResourceSearchHit> retrieveTitleScopedKeywordDocumentHits(
			UUID userId,
			UUID roomId,
			AgentSearchQueryAnalysis queryAnalysis,
			EnumSet<ProjectRoomGroundingSourceType> requestedSources,
			List<ResourceTitleMatch> titleMatches,
			List<String> retrievalFailures
	) {
		if (!requestedSources.contains(ProjectRoomGroundingSourceType.DOCUMENT) || titleMatches.isEmpty()) {
			return List.of();
		}
		List<String> keywords = queryAnalysis.keywords();
		if (keywords.isEmpty()) {
			return List.of();
		}
		List<UUID> resourceIds = titleMatches.stream()
				.map(match -> match.resource().id())
				.distinct()
				.toList();
		try {
			List<ResourceSearchHit> candidates = resourceSemanticSearchService.searchRoomSharedResourceKeywords(
					userId,
					roomId,
					resourceIds,
					keywords,
					titleScopedTopK(resourceIds.size())
			);
			List<ResourceSearchHit> safeCandidates = candidates == null ? List.of() : candidates;
			List<ResourceSearchHit> acceptedHits = safeCandidates.stream()
					.filter(hit -> hit.similarityScore() >= TITLE_SCOPED_KEYWORD_MIN_SCORE)
					.toList();
			resourceSearchMetrics.recordSelection(
					"keyword",
					"room_resources",
					safeCandidates.size(),
					acceptedHits.size()
			);
			return acceptedHits;
		} catch (RuntimeException exception) {
			log.warn("Project room title-scoped keyword document retrieval failed. userId={}, roomId={}",
					userId, roomId, exception);
			retrievalFailures.add("TITLE_SCOPED_KEYWORD_DOCUMENT_RETRIEVAL_FAILED");
			return List.of();
		}
	}

	private List<ResourceSearchHit> retrieveRepresentativeDocumentChunks(
			UUID userId,
			UUID roomId,
			EnumSet<ProjectRoomGroundingSourceType> requestedSources,
			boolean requireSemanticDocumentEvidence,
			boolean documentOverviewRequest,
			List<ResourceTitleMatch> titleMatches,
			List<String> retrievalFailures
	) {
		if (!requestedSources.contains(ProjectRoomGroundingSourceType.DOCUMENT)
				|| !requireSemanticDocumentEvidence
				|| !documentOverviewRequest
				|| titleMatches.isEmpty()) {
			return List.of();
		}
		List<UUID> resourceIds = titleMatches.stream()
				.map(match -> match.resource().id())
				.distinct()
				.toList();
		try {
			List<ResourceSearchHit> hits = resourceSemanticSearchService.loadRoomSharedResourceChunks(
					userId,
					roomId,
					resourceIds,
					titleScopedTopK(resourceIds.size())
			);
			List<ResourceSearchHit> safeHits = hits == null ? List.of() : hits;
			resourceSearchMetrics.recordFallback("representative", "room_resources", !safeHits.isEmpty());
			return safeHits;
		} catch (RuntimeException exception) {
			log.warn("Project room representative document chunk retrieval failed. userId={}, roomId={}",
					userId, roomId, exception);
			retrievalFailures.add("REPRESENTATIVE_DOCUMENT_RETRIEVAL_FAILED");
			return List.of();
		}
	}

	private List<ProjectRoomDocumentCandidate> toCandidates(
			List<ResourceSearchHit> hits,
			String retrievalMode,
			AgentSearchQueryAnalysis queryAnalysis,
			boolean titleScoped
	) {
		if (hits == null || hits.isEmpty()) {
			return List.of();
		}
		return hits.stream()
				.map(hit -> ProjectRoomDocumentCandidate.of(hit, retrievalMode, queryAnalysis, titleScoped))
				.toList();
	}

	private List<ProjectRoomDocumentCandidate> preferTitleMatchedDocumentCandidates(
			List<ProjectRoomDocumentCandidate> candidates,
			List<ResourceTitleMatch> titleMatches
	) {
		if (candidates.isEmpty() || titleMatches.isEmpty()) {
			return candidates;
		}
		List<UUID> matchedResourceIds = titleMatches.stream()
				.map(match -> match.resource().id())
				.distinct()
				.toList();
		List<ProjectRoomDocumentCandidate> titleMatchedCandidates = candidates.stream()
				.filter(candidate -> matchedResourceIds.contains(candidate.hit().resourceId()))
				.toList();
		if (!titleMatchedCandidates.isEmpty()) {
			return titleMatchedCandidates;
		}
		return hasStrongTitleMatch(titleMatches) ? List.of() : candidates;
	}

	private List<ResourceTitleMatch> retrieveDocumentTitleMatches(
			UUID userId,
			UUID roomId,
			String message,
			EnumSet<ProjectRoomGroundingSourceType> requestedSources,
			List<UUID> excludedResourceIds
	) {
		if (!requestedSources.contains(ProjectRoomGroundingSourceType.DOCUMENT)) {
			return List.of();
		}
		List<AgentQuerySupport.ResourceToken> queryTokens = AgentQuerySupport.resourceTokens(message);
		if (queryTokens.isEmpty()) {
			return List.of();
		}
		String normalizedMessage = AgentQuerySupport.compactResourceText(message);
		return resourcePublicService.getRecentRoomResources(userId, roomId, 30).stream()
				.filter(resource -> !excludedResourceIds.contains(resource.id()))
				.map(resource -> titleMatch(userId, resource, normalizedMessage, queryTokens))
				.filter(match -> match.score() >= RESOURCE_TITLE_MATCH_MIN_SCORE)
				.sorted(Comparator.comparingInt(ResourceTitleMatch::score).reversed())
				.limit(3)
				.toList();
	}

	private ResourceTitleMatch titleMatch(
			UUID userId,
			ResourceResult resource,
			String normalizedMessage,
			List<AgentQuerySupport.ResourceToken> queryTokens
	) {
		String normalizedTitle = AgentQuerySupport.compactResourceText(resource.title());
		int score = 0;
		if (!normalizedTitle.isBlank() && normalizedMessage.contains(normalizedTitle)) {
			score += 100;
		}
		for (AgentQuerySupport.ResourceToken token : queryTokens) {
			if (normalizedTitle.contains(token.value())) {
				score += token.weight();
			}
		}
		ResourceSummaryResult summary = resourcePublicService.findResourceSummary(userId, resource.id()).orElse(null);
		return new ResourceTitleMatch(resource, summary, score);
	}

	private boolean hasUsableTitleMatchEvidence(ResourceTitleMatch match) {
		ResourceStatus status = match.resource().status();
		return (status == ResourceStatus.READY || status == ResourceStatus.ANALYZED)
				&& match.summary() != null;
	}

	private List<ResourceSearchHit> selectDocumentHits(
			List<ResourceSearchHit> ragHits,
			List<ResourceSearchHit> titleScopedHits,
			List<ResourceTitleMatch> titleMatches
	) {
		if (titleMatches.isEmpty()) {
			return ragHits;
		}
		List<UUID> matchedResourceIds = titleMatches.stream()
				.map(match -> match.resource().id())
				.distinct()
				.toList();
		List<ResourceSearchHit> matchedHits = mergeDocumentHits(titleScopedHits, ragHits).stream()
				.filter(hit -> matchedResourceIds.contains(hit.resourceId()))
				.sorted(Comparator.comparingDouble(ResourceSearchHit::similarityScore).reversed())
				.toList();
		if (!matchedHits.isEmpty()) {
			return matchedHits;
		}
		if (hasStrongTitleMatch(titleMatches)) {
			return List.of();
		}
		return ragHits;
	}

	private List<ResourceSearchHit> mergeDocumentHits(List<ResourceSearchHit> first, List<ResourceSearchHit> second) {
		Map<UUID, ResourceSearchHit> hitsByEmbeddingId = new LinkedHashMap<>();
		for (ResourceSearchHit hit : first) {
			hitsByEmbeddingId.putIfAbsent(hit.embeddingId(), hit);
		}
		for (ResourceSearchHit hit : second) {
			hitsByEmbeddingId.putIfAbsent(hit.embeddingId(), hit);
		}
		return new ArrayList<>(hitsByEmbeddingId.values());
	}

	private Map<UUID, ProjectRoomDocumentCandidate> candidatesByEmbeddingId(
			List<ProjectRoomDocumentCandidate> candidates
	) {
		Map<UUID, ProjectRoomDocumentCandidate> candidatesByEmbeddingId = new LinkedHashMap<>();
		for (ProjectRoomDocumentCandidate candidate : candidates) {
			candidatesByEmbeddingId.put(candidate.hit().embeddingId(), candidate);
		}
		return candidatesByEmbeddingId;
	}

	private boolean hasStrongTitleMatch(List<ResourceTitleMatch> titleMatches) {
		return titleMatches.stream()
				.anyMatch(match -> match.score() >= STRONG_RESOURCE_TITLE_MATCH_SCORE);
	}

	private int titleScopedTopK(int resourceCount) {
		int baseTopK = agentRagProperties.topK() == null ? 5 : agentRagProperties.topK();
		return Math.max(baseTopK, baseTopK * Math.max(1, resourceCount) * TITLE_SCOPED_SEARCH_TOP_K_MULTIPLIER);
	}

	private double titleScopedMinSimilarity(AgentCommandMode mode) {
		double configuredMinSimilarity = minSimilarity(mode);
		if (mode == AgentCommandMode.SUGGEST) {
			return configuredMinSimilarity;
		}
		return Math.min(configuredMinSimilarity, TITLE_SCOPED_RELAXED_MIN_SIMILARITY);
	}

	private double documentMinSimilarity(AgentCommandMode mode, boolean requireSemanticDocumentEvidence) {
		double configuredMinSimilarity = minSimilarity(mode);
		if (mode == AgentCommandMode.SUGGEST || !requireSemanticDocumentEvidence) {
			return configuredMinSimilarity;
		}
		return Math.min(configuredMinSimilarity, PRECISE_GROUNDING_RELAXED_MIN_SIMILARITY);
	}

	private double minSimilarity(AgentCommandMode mode) {
		return mode == AgentCommandMode.SUGGEST
				? agentRagProperties.suggestMinSimilarity()
				: agentRagProperties.minSimilarity();
	}

	private List<String> keywordTokens(String searchQuery) {
		String normalized = AgentQuerySupport.compactResourceText(searchQuery);
		List<String> tokens = new ArrayList<>(AgentQuerySupport.requirementIdentifiers(searchQuery));
		if (normalized.isBlank()) {
			return tokens;
		}
		for (String token : normalized.split(" ")) {
			if (tokens.size() >= 5) {
				break;
			}
			if (token.length() >= 2 && !tokens.contains(token)) {
				tokens.add(token);
			}
		}
		if (tokens.isEmpty()) {
			return List.of();
		}
		return tokens;
	}

	private List<ResourceTitleMatch> excludeTitleMatchesAlreadyCoveredByRag(
			List<ResourceTitleMatch> titleMatches,
			List<ResourceSearchHit> ragHits
	) {
		if (titleMatches.isEmpty() || ragHits.isEmpty()) {
			return titleMatches;
		}
		List<UUID> ragResourceIds = ragHits.stream()
				.map(ResourceSearchHit::resourceId)
				.distinct()
				.toList();
		return titleMatches.stream()
				.filter(match -> !ragResourceIds.contains(match.resource().id()))
				.toList();
	}

	private void appendDocumentEvidence(
			List<ResourceSearchHit> ragHits,
			Map<UUID, ProjectRoomDocumentCandidate> candidatesByEmbeddingId,
			Map<UUID, String> resourceTitles,
			List<ProjectRoomGroundingEvidence> evidenceItems,
			StringBuilder prompt
	) {
		for (ResourceSearchHit hit : ragHits) {
			ProjectRoomDocumentCandidate candidate = candidatesByEmbeddingId.get(hit.embeddingId());
			String title = title(hit.originalName(), resourceTitles.get(hit.resourceId()));
			if (title == null || title.isBlank()) {
				continue;
			}
			Map<String, Object> metadata = new LinkedHashMap<>();
			metadata.put("retrievalMode", candidate == null ? "SEMANTIC" : candidate.retrievalMode());
			metadata.put("chunkIndex", hit.chunkIndex());
			metadata.put("pageNumber", hit.pageNumber());
			metadata.put("startLine", hit.startLine());
			metadata.put("endLine", hit.endLine());
			metadata.put("startOffset", hit.startOffset());
			metadata.put("endOffset", hit.endOffset());
			metadata.put("originalName", hit.originalName());
			metadata.put("title", title);
			metadata.put("similarityScore", hit.similarityScore());
			if (candidate != null) {
				metadata.put("fusionScore", candidate.fusionScore());
				metadata.put("matchedKeywords", candidate.matchedKeywords());
				metadata.put("matchReason", candidate.matchReason());
			}
			metadata.put("quote", quote(hit.chunkText()));
			evidenceItems.add(new ProjectRoomGroundingEvidence(
					ProjectRoomGroundingSourceType.DOCUMENT,
					hit.resourceId(),
					metadata
			));
			prompt.append("[DOCUMENT]\n")
					.append("resourceId=").append(hit.resourceId()).append('\n')
					.append("chunkIndex=").append(hit.chunkIndex()).append('\n')
					.append("pageNumber=").append(hit.pageNumber()).append('\n')
					.append("startLine=").append(hit.startLine()).append('\n')
					.append("endLine=").append(hit.endLine()).append('\n')
					.append("similarityScore=").append(hit.similarityScore()).append('\n')
					.append("fusionScore=").append(candidate == null ? hit.similarityScore() : candidate.fusionScore()).append('\n')
					.append("matchReason=").append(candidate == null ? "SEMANTIC" : candidate.matchReason()).append('\n')
					.append("chunkText=\n")
					.append(hit.chunkText()).append("\n\n");
		}
	}

	private void appendResourceTitleEvidence(
			List<ResourceTitleMatch> titleMatches,
			List<ProjectRoomGroundingEvidence> evidenceItems,
			StringBuilder prompt
	) {
		for (ResourceTitleMatch match : titleMatches) {
			ResourceResult resource = match.resource();
			Map<String, Object> metadata = new LinkedHashMap<>();
			metadata.put("retrievalMode", "TITLE_MATCH");
			metadata.put("title", resource.title());
			metadata.put("kind", resource.kind());
			metadata.put("status", resource.status());
			metadata.put("matchScore", match.score());
			evidenceItems.add(new ProjectRoomGroundingEvidence(
					ProjectRoomGroundingSourceType.DOCUMENT,
					resource.id(),
					metadata
			));
			prompt.append("[DOCUMENT]\n")
					.append("retrievalMode=TITLE_MATCH\n")
					.append("resourceId=").append(resource.id()).append('\n')
					.append("title=").append(resource.title()).append('\n')
					.append("kind=").append(resource.kind()).append('\n')
					.append("status=").append(resource.status()).append('\n')
					.append("matchScore=").append(match.score()).append('\n');
			appendSummary(match.summary(), prompt);
			prompt.append('\n');
		}
	}

	private void appendRecentResourceSummaryEvidence(
			UUID userId,
			UUID roomId,
			EnumSet<ProjectRoomGroundingSourceType> requestedSources,
			List<ProjectRoomGroundingEvidence> evidenceItems,
			StringBuilder prompt
	) {
		if (!requestedSources.contains(ProjectRoomGroundingSourceType.DOCUMENT) || hasDocumentEvidence(evidenceItems)) {
			return;
		}
		for (ResourceSummaryResult summary : resourcePublicService.getRecentRoomSummaries(
				userId,
				roomId,
				Math.min(DEFAULT_CONTEXT_LIMIT, 5)
		)) {
			String title = resourceTitle(userId, summary.resourceId());
			if (title == null || title.isBlank()) {
				continue;
			}
			Map<String, Object> metadata = new LinkedHashMap<>();
			metadata.put("retrievalMode", "RECENT_SUMMARY");
			metadata.put("title", title);
			metadata.put("status", summary.status());
			metadata.put("updatedAt", summary.updatedAt());
			evidenceItems.add(new ProjectRoomGroundingEvidence(
					ProjectRoomGroundingSourceType.DOCUMENT,
					summary.resourceId(),
					metadata
			));
			prompt.append("[DOCUMENT]\n")
					.append("retrievalMode=RECENT_SUMMARY\n")
					.append("resourceId=").append(summary.resourceId()).append('\n');
			appendSummary(summary, prompt);
			prompt.append('\n');
		}
	}

	private boolean hasDocumentEvidence(List<ProjectRoomGroundingEvidence> evidenceItems) {
		return evidenceItems.stream()
				.anyMatch(evidence -> evidence.sourceType() == ProjectRoomGroundingSourceType.DOCUMENT);
	}

	private void appendSummary(ResourceSummaryResult summary, StringBuilder prompt) {
		if (summary == null) {
			prompt.append("summaryJson=\n").append("분석 요약이 아직 없습니다.\n");
			return;
		}
		prompt.append("summaryJson=\n")
				.append(nullToEmpty(summary.summaryJson())).append('\n')
				.append("checklistJson=\n")
				.append(nullToEmpty(summary.checklistJson())).append('\n');
	}

	private void appendTaskEvidence(
			UUID roomId,
			EnumSet<ProjectRoomGroundingSourceType> requestedSources,
			AgentQuerySupport.WorkStateIntent workStateIntent,
			List<ProjectRoomGroundingEvidence> evidenceItems,
			StringBuilder prompt
	) {
		if (!requestedSources.contains(ProjectRoomGroundingSourceType.TASK)) {
			return;
		}
		for (TaskResult task : prioritizedTasks(
				taskPublicService.getRecentRoomTasks(roomId, DEFAULT_CONTEXT_LIMIT * 2),
				workStateIntent
		)) {
			Map<String, Object> metadata = new LinkedHashMap<>();
			metadata.put("retrievalMode", "MANAGEMENT_CONTEXT");
			metadata.put("workState", taskWorkState(task));
			metadata.put("title", task.title());
			metadata.put("status", task.status());
			metadata.put("assigneeUserId", task.assigneeUserId());
			metadata.put("wbsItemId", task.wbsItemId());
			metadata.put("dueAt", task.dueAt());
			evidenceItems.add(new ProjectRoomGroundingEvidence(ProjectRoomGroundingSourceType.TASK, task.id(), metadata));
			prompt.append("[TASK]\n")
					.append("retrievalMode=MANAGEMENT_CONTEXT\n")
					.append("workState=").append(taskWorkState(task)).append('\n')
					.append("taskId=").append(task.id()).append('\n')
					.append("title=").append(task.title()).append('\n')
					.append("status=").append(task.status()).append('\n')
					.append("assigneeUserId=").append(task.assigneeUserId()).append('\n')
					.append("wbsItemId=").append(task.wbsItemId()).append('\n')
					.append("dueAt=").append(task.dueAt()).append('\n')
					.append("description=").append(nullToEmpty(task.description())).append("\n\n");
		}
	}

	private void appendWbsEvidence(
			UUID roomId,
			EnumSet<ProjectRoomGroundingSourceType> requestedSources,
			AgentQuerySupport.WorkStateIntent workStateIntent,
			List<ProjectRoomGroundingEvidence> evidenceItems,
			StringBuilder prompt
	) {
		if (!requestedSources.contains(ProjectRoomGroundingSourceType.WBS)) {
			return;
		}
		for (WbsItemResult wbsItem : prioritizedWbsItems(
				wbsItemPublicService.getRoomContextItems(roomId, DEFAULT_CONTEXT_LIMIT * 2),
				workStateIntent
		)) {
			Map<String, Object> metadata = new LinkedHashMap<>();
			metadata.put("retrievalMode", "MANAGEMENT_CONTEXT");
			metadata.put("workState", wbsWorkState(wbsItem));
			metadata.put("title", wbsItem.title());
			metadata.put("status", wbsItem.status());
			metadata.put("parentId", wbsItem.parentId());
			metadata.put("orderNo", wbsItem.orderNo());
			evidenceItems.add(new ProjectRoomGroundingEvidence(ProjectRoomGroundingSourceType.WBS, wbsItem.id(), metadata));
			prompt.append("[WBS]\n")
					.append("retrievalMode=MANAGEMENT_CONTEXT\n")
					.append("workState=").append(wbsWorkState(wbsItem)).append('\n')
					.append("wbsItemId=").append(wbsItem.id()).append('\n')
					.append("title=").append(wbsItem.title()).append('\n')
					.append("status=").append(wbsItem.status()).append('\n')
					.append("parentId=").append(wbsItem.parentId()).append('\n')
					.append("orderNo=").append(wbsItem.orderNo()).append("\n\n");
		}
	}

	private void appendScheduleEvidence(
			UUID roomId,
			EnumSet<ProjectRoomGroundingSourceType> requestedSources,
			List<ProjectRoomGroundingEvidence> evidenceItems,
			StringBuilder prompt
	) {
		if (!requestedSources.contains(ProjectRoomGroundingSourceType.SCHEDULE)) {
			return;
		}
		Instant now = Instant.now();
		Instant from = now.minus(SCHEDULE_LOOKBACK);
		Instant to = now.plus(SCHEDULE_LOOKAHEAD);
		for (ScheduleResult schedule : schedulePublicService.getRoomSchedulesBetween(roomId, from, to).stream()
				.limit(DEFAULT_CONTEXT_LIMIT)
				.toList()) {
			Map<String, Object> metadata = new LinkedHashMap<>();
			metadata.put("retrievalMode", "MANAGEMENT_CONTEXT");
			metadata.put("title", schedule.title());
			metadata.put("startsAt", schedule.startsAt());
			metadata.put("endsAt", schedule.endsAt());
			metadata.put("allDay", schedule.allDay());
			metadata.put("taskId", schedule.taskId());
			metadata.put("wbsItemId", schedule.wbsItemId());
			evidenceItems.add(new ProjectRoomGroundingEvidence(ProjectRoomGroundingSourceType.SCHEDULE, schedule.id(), metadata));
			prompt.append("[SCHEDULE]\n")
					.append("retrievalMode=MANAGEMENT_CONTEXT\n")
					.append("scheduleId=").append(schedule.id()).append('\n')
					.append("title=").append(schedule.title()).append('\n')
					.append("startsAt=").append(schedule.startsAt()).append('\n')
					.append("endsAt=").append(schedule.endsAt()).append('\n')
					.append("allDay=").append(schedule.allDay()).append('\n')
					.append("taskId=").append(schedule.taskId()).append('\n')
					.append("wbsItemId=").append(schedule.wbsItemId()).append("\n\n");
		}
	}

	private void appendAgentSuggestionEvidence(
			UUID userId,
			UUID roomId,
			EnumSet<ProjectRoomGroundingSourceType> requestedSources,
			List<ProjectRoomGroundingEvidence> evidenceItems,
			StringBuilder prompt
	) {
		if (!requestedSources.contains(ProjectRoomGroundingSourceType.AGENT_SUGGESTION)) {
			return;
		}
		for (AgentSuggestionResponse suggestion : agentSuggestionPublicService
				.getRecentRoomSuggestions(userId, roomId, DEFAULT_CONTEXT_LIMIT)) {
			Map<String, Object> metadata = new LinkedHashMap<>();
			metadata.put("retrievalMode", "MANAGEMENT_CONTEXT");
			metadata.put("type", suggestion.suggestionType());
			metadata.put("status", suggestion.status());
			metadata.put("resourceId", suggestion.resourceId());
			metadata.put("payload", suggestion.payloadJson());
			evidenceItems.add(new ProjectRoomGroundingEvidence(
					ProjectRoomGroundingSourceType.AGENT_SUGGESTION,
					suggestion.suggestionId(),
					metadata
			));
			prompt.append("[AGENT_SUGGESTION]\n")
					.append("suggestionId=").append(suggestion.suggestionId()).append('\n')
					.append("type=").append(suggestion.suggestionType()).append('\n')
					.append("status=").append(suggestion.status()).append('\n')
					.append("resourceId=").append(suggestion.resourceId()).append('\n')
					.append("payload=").append(suggestion.payloadJson()).append("\n\n");
		}
	}

	private List<TaskResult> prioritizedTasks(
			List<TaskResult> tasks,
			AgentQuerySupport.WorkStateIntent workStateIntent
	) {
		return tasks.stream()
				.sorted(Comparator.comparingInt(task -> workStatePriority(taskWorkState(task), workStateIntent)))
				.limit(DEFAULT_CONTEXT_LIMIT)
				.toList();
	}

	private List<WbsItemResult> prioritizedWbsItems(
			List<WbsItemResult> items,
			AgentQuerySupport.WorkStateIntent workStateIntent
	) {
		return items.stream()
				.sorted(Comparator.comparingInt(item -> workStatePriority(wbsWorkState(item), workStateIntent)))
				.limit(DEFAULT_CONTEXT_LIMIT)
				.toList();
	}

	private int workStatePriority(String workState, AgentQuerySupport.WorkStateIntent workStateIntent) {
		if (workStateIntent == AgentQuerySupport.WorkStateIntent.COMPLETED) {
			return "COMPLETED".equals(workState) ? 0 : 1;
		}
		if (workStateIntent == AgentQuerySupport.WorkStateIntent.ACTIVE) {
			return "ACTIVE".equals(workState) ? 0 : 1;
		}
		return "ACTIVE".equals(workState) ? 0 : 1;
	}

	private String taskWorkState(TaskResult task) {
		return task.status() == TaskStatus.DONE ? "COMPLETED" : "ACTIVE";
	}

	private String wbsWorkState(WbsItemResult item) {
		return item.status() == WbsStatus.DONE ? "COMPLETED" : "ACTIVE";
	}

	private double maxSimilarity(List<ResourceSearchHit> ragHits) {
		return ragHits.stream()
				.map(ResourceSearchHit::similarityScore)
				.max(Comparator.naturalOrder())
				.orElse(0.0D);
	}

	private Map<UUID, String> resourceTitles(UUID userId, List<UUID> resourceIds) {
		Map<UUID, String> titles = new LinkedHashMap<>();
		for (UUID resourceId : resourceIds) {
			String title = resourceTitle(userId, resourceId);
			if (title != null && !title.isBlank()) {
				titles.put(resourceId, title);
			}
		}
		return titles;
	}

	private List<ResourceSearchHit> titleResolvedDocumentHits(
			List<ResourceSearchHit> ragHits,
			Map<UUID, String> resourceTitles
	) {
		return ragHits.stream()
				.filter(hit -> {
					String title = title(hit.originalName(), resourceTitles.get(hit.resourceId()));
					if (title != null && !title.isBlank()) {
						return true;
					}
					log.warn("Dropping document grounding hit without resolvable title. resourceId={}, chunkIndex={}",
							hit.resourceId(), hit.chunkIndex());
					return false;
				})
				.toList();
	}

	private String resourceTitle(UUID userId, UUID resourceId) {
		try {
			return resourcePublicService.getReadableResource(userId, resourceId).title();
		} catch (RuntimeException exception) {
			log.warn("Failed to resolve resource title for grounding citation. userId={}, resourceId={}",
					userId, resourceId, exception);
			return null;
		}
	}

	private String title(String originalName, String resourceTitle) {
		if (originalName != null && !originalName.isBlank()) {
			return originalName;
		}
		return resourceTitle;
	}

	private String quote(String value) {
		String text = nullToEmpty(value).replaceAll("\\s+", " ").trim();
		return text.length() <= 500 ? text : text.substring(0, 500).trim();
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private record ResourceTitleMatch(
			ResourceResult resource,
			ResourceSummaryResult summary,
			int score
	) {
	}
}
