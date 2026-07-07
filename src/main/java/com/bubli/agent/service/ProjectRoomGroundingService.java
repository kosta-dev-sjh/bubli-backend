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
import com.bubli.resource.service.ResourceSemanticSearchPublicService;
import com.bubli.resource.type.ResourceSearchScope;
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
	private static final Duration SCHEDULE_LOOKBACK = Duration.ofDays(7);
	private static final Duration SCHEDULE_LOOKAHEAD = Duration.ofDays(30);

	private final ResourceSemanticSearchPublicService resourceSemanticSearchService;
	private final ResourcePublicService resourcePublicService;
	private final AgentRagProperties agentRagProperties;
	private final TaskPublicService taskPublicService;
	private final WbsItemPublicService wbsItemPublicService;
	private final SchedulePublicService schedulePublicService;
	private final AgentSuggestionPublicService agentSuggestionPublicService;

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

			String searchQuery = AgentQuerySupport.searchQuery(message);
			AgentQuerySupport.WorkStateIntent workStateIntent = AgentQuerySupport.workStateIntent(message);
			List<ResourceSearchHit> ragHits = retrieveDocumentHits(userId, roomId, searchQuery, mode, requestedSources);
			List<ResourceTitleMatch> titleMatches = retrieveDocumentTitleMatches(
					userId,
					roomId,
					message,
					requestedSources,
					ragHits
			);
			List<ProjectRoomGroundingEvidence> evidenceItems = new ArrayList<>();
			StringBuilder prompt = new StringBuilder();

			appendDocumentEvidence(ragHits, evidenceItems, prompt);
			appendResourceTitleEvidence(titleMatches, evidenceItems, prompt);
			appendRecentResourceSummaryEvidence(userId, roomId, requestedSources, evidenceItems, prompt);
			appendTaskEvidence(roomId, requestedSources, workStateIntent, evidenceItems, prompt);
			appendWbsEvidence(roomId, requestedSources, workStateIntent, evidenceItems, prompt);
			appendScheduleEvidence(roomId, requestedSources, evidenceItems, prompt);
			appendAgentSuggestionEvidence(userId, roomId, requestedSources, evidenceItems, prompt);

			if (evidenceItems.isEmpty()) {
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
			return ProjectRoomGroundingContext.ungrounded();
		}
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
			EnumSet<ProjectRoomGroundingSourceType> requestedSources
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
			return List.of();
		}
		double minSimilarity = mode == AgentCommandMode.SUGGEST
				? agentRagProperties.suggestMinSimilarity()
				: agentRagProperties.minSimilarity();
		return hits.stream()
				.filter(hit -> hit.similarityScore() >= minSimilarity)
				.toList();
	}

	private List<ResourceTitleMatch> retrieveDocumentTitleMatches(
			UUID userId,
			UUID roomId,
			String message,
			EnumSet<ProjectRoomGroundingSourceType> requestedSources,
			List<ResourceSearchHit> ragHits
	) {
		if (!requestedSources.contains(ProjectRoomGroundingSourceType.DOCUMENT)) {
			return List.of();
		}
		List<AgentQuerySupport.ResourceToken> queryTokens = AgentQuerySupport.resourceTokens(message);
		if (queryTokens.isEmpty()) {
			return List.of();
		}
		List<UUID> ragResourceIds = ragHits.stream()
				.map(ResourceSearchHit::resourceId)
				.distinct()
				.toList();
		String normalizedMessage = AgentQuerySupport.compactResourceText(message);
		return resourcePublicService.getRecentRoomResources(userId, roomId, 30).stream()
				.filter(resource -> !ragResourceIds.contains(resource.id()))
				.map(resource -> titleMatch(userId, resource, normalizedMessage, queryTokens))
				.filter(match -> match.score() >= 4)
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

	private void appendDocumentEvidence(
			List<ResourceSearchHit> ragHits,
			List<ProjectRoomGroundingEvidence> evidenceItems,
			StringBuilder prompt
	) {
		for (ResourceSearchHit hit : ragHits) {
			Map<String, Object> metadata = new LinkedHashMap<>();
			metadata.put("retrievalMode", "SEMANTIC");
			metadata.put("chunkIndex", hit.chunkIndex());
			metadata.put("pageNumber", hit.pageNumber());
			metadata.put("similarityScore", hit.similarityScore());
			evidenceItems.add(new ProjectRoomGroundingEvidence(
					ProjectRoomGroundingSourceType.DOCUMENT,
					hit.resourceId(),
					metadata
			));
			prompt.append("[DOCUMENT]\n")
					.append("resourceId=").append(hit.resourceId()).append('\n')
					.append("chunkIndex=").append(hit.chunkIndex()).append('\n')
					.append("pageNumber=").append(hit.pageNumber()).append('\n')
					.append("similarityScore=").append(hit.similarityScore()).append('\n')
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
			Map<String, Object> metadata = new LinkedHashMap<>();
			metadata.put("retrievalMode", "RECENT_SUMMARY");
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
