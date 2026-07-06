package com.bubli.agent.service;

import com.bubli.agent.config.AgentRagProperties;
import com.bubli.agent.dto.AgentSuggestionResponse;
import com.bubli.agent.dto.ProjectRoomGroundingContext;
import com.bubli.agent.dto.ProjectRoomGroundingEvidence;
import com.bubli.agent.dto.ProjectRoomGroundingSourceType;
import com.bubli.agent.type.AgentCommandMode;
import com.bubli.resource.dto.ResourceSearchHit;
import com.bubli.resource.service.ResourceSemanticSearchPublicService;
import com.bubli.resource.type.ResourceSearchScope;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.service.SchedulePublicService;
import com.bubli.work.task.dto.TaskResult;
import com.bubli.work.task.service.TaskPublicService;
import com.bubli.work.wbs.dto.WbsItemResult;
import com.bubli.work.wbs.service.WbsItemPublicService;
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
import java.util.Locale;
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

			List<ResourceSearchHit> ragHits = retrieveDocumentHits(userId, roomId, message, mode, requestedSources);
			List<ProjectRoomGroundingEvidence> evidenceItems = new ArrayList<>();
			StringBuilder prompt = new StringBuilder();

			appendDocumentEvidence(ragHits, evidenceItems, prompt);
			appendTaskEvidence(roomId, requestedSources, evidenceItems, prompt);
			appendWbsEvidence(roomId, requestedSources, evidenceItems, prompt);
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
		String normalized = normalize(message);
		EnumSet<ProjectRoomGroundingSourceType> sources = EnumSet.noneOf(ProjectRoomGroundingSourceType.class);
		if (isDocumentSourceRequest(normalized)) {
			sources.add(ProjectRoomGroundingSourceType.DOCUMENT);
		}
		if (isTaskSourceRequest(normalized, mode)) {
			sources.add(ProjectRoomGroundingSourceType.TASK);
		}
		if (isWbsSourceRequest(normalized, mode)) {
			sources.add(ProjectRoomGroundingSourceType.WBS);
		}
		if (isScheduleSourceRequest(normalized)) {
			sources.add(ProjectRoomGroundingSourceType.SCHEDULE);
		}
		if (isAgentSuggestionSourceRequest(normalized)) {
			sources.add(ProjectRoomGroundingSourceType.AGENT_SUGGESTION);
		}
		return sources;
	}

	private boolean isDocumentSourceRequest(String normalized) {
		return containsAny(normalized, "계약", "계약서", "문서", "자료", "파일", "요구사항", "resource", "document",
				"file", "contract", "agreement", "material", "資料", "文書", "ファイル", "契約", "契約書", "要件");
	}

	private boolean isTaskSourceRequest(String normalized, AgentCommandMode mode) {
		boolean hasTaskTerm = containsAny(normalized, "작업", "태스크", "할 일", "할일", "task", "タスク", "作業")
				|| containsAny(normalized, "todo", "to-do") && containsAny(normalized, "미완료", "기존", "현재", "기준",
				"보고", "목록", "정리", "unfinished", "existing", "current", "未完了", "既存", "現在");
		return hasTaskTerm && (mode == AgentCommandMode.ANSWER || hasSourceIntent(normalized));
	}

	private boolean isWbsSourceRequest(String normalized, AgentCommandMode mode) {
		boolean hasWbsTerm = containsAny(normalized, "wbs", "work breakdown", "업무분해", "作業分解");
		return hasWbsTerm && (mode == AgentCommandMode.ANSWER || hasSourceIntent(normalized));
	}

	private boolean isScheduleSourceRequest(String normalized) {
		return containsAny(normalized, "일정", "스케줄", "캘린더", "오늘", "내일", "이번 주", "다음 주",
				"schedule", "calendar", "today", "tomorrow", "this week", "next week",
				"予定", "スケジュール", "カレンダー", "今日", "明日", "今週", "来週");
	}

	private boolean isAgentSuggestionSourceRequest(String normalized) {
		return containsAny(normalized, "ai 후보", "ai후보", "후보함", "후보", "제안함", "suggestion", "candidate",
				"draft", "ai候補", "候補");
	}

	private boolean hasSourceIntent(String normalized) {
		return containsAny(normalized, "기준", "바탕", "기반", "보고", "토대로", "현재", "기존", "미완료", "완료",
				"목록", "정리", "참고", "based on", "from", "using", "current", "existing", "基準", "もと",
				"基づ", "見て", "現在", "既存", "未完了", "一覧");
	}

	private List<ResourceSearchHit> retrieveDocumentHits(
			UUID userId,
			UUID roomId,
			String message,
			AgentCommandMode mode,
			EnumSet<ProjectRoomGroundingSourceType> requestedSources
	) {
		if (!requestedSources.contains(ProjectRoomGroundingSourceType.DOCUMENT) || !agentRagProperties.enabled()) {
			return List.of();
		}
		List<ResourceSearchHit> hits = resourceSemanticSearchService.search(
				userId,
				ResourceSearchScope.ROOM_SHARED,
				roomId,
				message,
				agentRagProperties.topK()
		);
		double minSimilarity = mode == AgentCommandMode.SUGGEST
				? agentRagProperties.suggestMinSimilarity()
				: agentRagProperties.minSimilarity();
		return hits.stream()
				.filter(hit -> hit.similarityScore() >= minSimilarity)
				.toList();
	}

	private void appendDocumentEvidence(
			List<ResourceSearchHit> ragHits,
			List<ProjectRoomGroundingEvidence> evidenceItems,
			StringBuilder prompt
	) {
		for (ResourceSearchHit hit : ragHits) {
			Map<String, Object> metadata = new LinkedHashMap<>();
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

	private void appendTaskEvidence(
			UUID roomId,
			EnumSet<ProjectRoomGroundingSourceType> requestedSources,
			List<ProjectRoomGroundingEvidence> evidenceItems,
			StringBuilder prompt
	) {
		if (!requestedSources.contains(ProjectRoomGroundingSourceType.TASK)) {
			return;
		}
		for (TaskResult task : taskPublicService.getRecentRoomTasks(roomId, DEFAULT_CONTEXT_LIMIT)) {
			Map<String, Object> metadata = new LinkedHashMap<>();
			metadata.put("title", task.title());
			metadata.put("status", task.status());
			metadata.put("assigneeUserId", task.assigneeUserId());
			metadata.put("wbsItemId", task.wbsItemId());
			metadata.put("dueAt", task.dueAt());
			evidenceItems.add(new ProjectRoomGroundingEvidence(ProjectRoomGroundingSourceType.TASK, task.id(), metadata));
			prompt.append("[TASK]\n")
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
			List<ProjectRoomGroundingEvidence> evidenceItems,
			StringBuilder prompt
	) {
		if (!requestedSources.contains(ProjectRoomGroundingSourceType.WBS)) {
			return;
		}
		for (WbsItemResult wbsItem : wbsItemPublicService.getRoomContextItems(roomId, DEFAULT_CONTEXT_LIMIT)) {
			Map<String, Object> metadata = new LinkedHashMap<>();
			metadata.put("title", wbsItem.title());
			metadata.put("status", wbsItem.status());
			metadata.put("parentId", wbsItem.parentId());
			metadata.put("orderNo", wbsItem.orderNo());
			evidenceItems.add(new ProjectRoomGroundingEvidence(ProjectRoomGroundingSourceType.WBS, wbsItem.id(), metadata));
			prompt.append("[WBS]\n")
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
			metadata.put("title", schedule.title());
			metadata.put("startsAt", schedule.startsAt());
			metadata.put("endsAt", schedule.endsAt());
			metadata.put("allDay", schedule.allDay());
			metadata.put("taskId", schedule.taskId());
			metadata.put("wbsItemId", schedule.wbsItemId());
			evidenceItems.add(new ProjectRoomGroundingEvidence(ProjectRoomGroundingSourceType.SCHEDULE, schedule.id(), metadata));
			prompt.append("[SCHEDULE]\n")
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

	private double maxSimilarity(List<ResourceSearchHit> ragHits) {
		return ragHits.stream()
				.map(ResourceSearchHit::similarityScore)
				.max(Comparator.naturalOrder())
				.orElse(0.0D);
	}

	private boolean containsAny(String value, String... candidates) {
		for (String candidate : candidates) {
			if (value.contains(candidate.toLowerCase(Locale.ROOT))) {
				return true;
			}
		}
		return false;
	}

	private String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
