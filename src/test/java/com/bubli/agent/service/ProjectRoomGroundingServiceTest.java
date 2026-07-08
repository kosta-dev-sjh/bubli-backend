package com.bubli.agent.service;

import com.bubli.agent.config.AgentRagProperties;
import com.bubli.agent.dto.AgentSuggestionResponse;
import com.bubli.agent.dto.ProjectRoomGroundingSourceType;
import com.bubli.agent.type.AgentCommandMode;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.dto.ResourceSearchHit;
import com.bubli.resource.dto.ResourceSummaryResult;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.resource.service.ResourceSemanticSearchPublicService;
import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceSearchScope;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.resource.type.ResourceSummaryStatus;
import com.bubli.resource.type.ResourceVisibility;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.service.SchedulePublicService;
import com.bubli.work.schedule.type.ScheduleSyncStatus;
import com.bubli.work.task.dto.TaskResult;
import com.bubli.work.task.service.TaskPublicService;
import com.bubli.work.task.type.TaskStatus;
import com.bubli.work.wbs.dto.WbsItemResult;
import com.bubli.work.wbs.service.WbsItemPublicService;
import com.bubli.work.wbs.type.WbsStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectRoomGroundingServiceTest {

	@Test
	void documentQuestionUsesRoomSharedSemanticSearch() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		ResourceSemanticSearchPublicService searchService = mock(ResourceSemanticSearchPublicService.class);
		ResourceSearchHit hit = hit(resourceId, "contract text", 0.9D);

		when(searchService.search(userId, ResourceSearchScope.ROOM_SHARED, roomId, "계약서", 5))
				.thenReturn(List.of(hit));

		var context = service(searchService).retrieve(
				userId,
				roomId,
				"계약서 내용 알려줘",
				"ko-KR",
				AgentCommandMode.ANSWER
		);

		verify(searchService).search(eq(userId), eq(ResourceSearchScope.ROOM_SHARED), eq(roomId), eq("계약서"), eq(5));
		assertThat(context.grounded()).isTrue();
		assertThat(context.sourceTypes()).containsExactly(ProjectRoomGroundingSourceType.DOCUMENT);
		assertThat(context.resourceIds()).containsExactly(resourceId);
		assertThat(context.promptBlock())
				.contains("[DOCUMENT]")
				.contains("startLine=10")
				.contains("endLine=12")
				.contains("contract text");
	}

	@Test
	void documentTitleQuestionUsesMatchedResourceSummaryWhenSemanticSearchIsEmpty() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		ResourceSemanticSearchPublicService searchService = mock(ResourceSemanticSearchPublicService.class);
		ResourcePublicService resourcePublicService = mock(ResourcePublicService.class);
		ResourceResult resource = resource(
				resourceId,
				userId,
				roomId,
				"02-design-outsourcing-requirements-example.pdf"
		);
		ResourceSummaryResult summary = resourceSummary(
				resourceId,
				"{\"summary\":\"브랜드 가이드, 화면 시안, 검수 일정 정리가 필요합니다.\"}"
		);

		when(searchService.search(userId, ResourceSearchScope.ROOM_SHARED, roomId,
				"02 design outsourcing", 5))
				.thenReturn(List.of());
		when(resourcePublicService.getRecentRoomResources(userId, roomId, 30))
				.thenReturn(List.of(resource));
		when(resourcePublicService.findResourceSummary(userId, resourceId))
				.thenReturn(Optional.of(summary));

		var context = service(searchService, resourcePublicService).retrieve(
				userId,
				roomId,
				"/bubli 02 design outsourcing pdf 주요 내용",
				"ko-KR",
				AgentCommandMode.ANSWER
		);

		assertThat(context.grounded()).isTrue();
		assertThat(context.sourceTypes()).containsExactly(ProjectRoomGroundingSourceType.DOCUMENT);
		assertThat(context.resourceIds()).containsExactly(resourceId);
		assertThat(context.ragHits()).isEmpty();
		assertThat(context.promptBlock())
				.contains("retrievalMode=TITLE_MATCH")
				.contains("02-design-outsourcing-requirements-example.pdf")
				.contains("브랜드 가이드");
	}

	@Test
	void documentTitleHintRestrictsSemanticHitsToMatchedResource() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID targetResourceId = UUID.randomUUID();
		UUID otherResourceId = UUID.randomUUID();
		ResourceSemanticSearchPublicService searchService = mock(ResourceSemanticSearchPublicService.class);
		ResourcePublicService resourcePublicService = mock(ResourcePublicService.class);
		TaskPublicService taskPublicService = mock(TaskPublicService.class);
		ResourceResult targetResource = resource(
				targetResourceId,
				userId,
				roomId,
				"01_UI디자이너_김서연.pdf"
		);
		ResourceResult otherResource = resource(
				otherResourceId,
				userId,
				roomId,
				"02_프론트엔드개발자_이준호.pdf"
		);

		when(searchService.search(eq(userId), eq(ResourceSearchScope.ROOM_SHARED), eq(roomId), any(String.class), eq(5)))
				.thenReturn(List.of(
						hit(otherResourceId, "이준호 프론트엔드 개발 계약서", 0.94D),
						hit(targetResourceId, "김서연 UI/UX 디자인 계약서", 0.91D)
				));
		when(resourcePublicService.getRecentRoomResources(userId, roomId, 30))
				.thenReturn(List.of(targetResource, otherResource));
		when(resourcePublicService.findResourceSummary(eq(userId), any(UUID.class)))
				.thenReturn(Optional.empty());
		when(taskPublicService.getRecentRoomTasks(roomId, 20)).thenReturn(List.of());

		var context = new ProjectRoomGroundingService(
				searchService,
				resourcePublicService,
				new AgentRagProperties(true, 5, 0.72D, 0.0D),
				taskPublicService,
				mock(WbsItemPublicService.class),
				mock(SchedulePublicService.class),
				mock(AgentSuggestionPublicService.class)
		).retrieve(
				userId,
				roomId,
				"/bubli todo 김서연 파일 바탕으로 todo 후보 만들어줘",
				"ko-KR",
				AgentCommandMode.SUGGEST
		);

		assertThat(context.grounded()).isTrue();
		assertThat(context.resourceIds()).containsExactly(targetResourceId);
		assertThat(context.promptBlock())
				.contains("김서연 UI/UX 디자인 계약서")
				.doesNotContain("이준호 프론트엔드 개발 계약서");
	}

	@Test
	void todoQuestionUsesRecentRoomTasks() {
		UUID roomId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		TaskPublicService taskPublicService = mock(TaskPublicService.class);

		when(taskPublicService.getRecentRoomTasks(roomId, 20))
				.thenReturn(List.of(task(taskId, roomId, "미완료 계약 검토")));

		var context = service(taskPublicService).retrieve(
				UUID.randomUUID(),
				roomId,
				"현재 미완료 TODO 알려줘",
				"ko-KR",
				AgentCommandMode.ANSWER
		);

		verify(taskPublicService).getRecentRoomTasks(roomId, 20);
		assertThat(context.grounded()).isTrue();
		assertThat(context.sourceTypes()).containsExactly(ProjectRoomGroundingSourceType.TASK);
		assertThat(context.taskIds()).containsExactly(taskId);
		assertThat(context.promptBlock()).contains("[TASK]").contains("미완료 계약 검토");
	}

	@Test
	void completedTaskQuestionPrioritizesDoneTasks() {
		UUID roomId = UUID.randomUUID();
		UUID activeTaskId = UUID.randomUUID();
		UUID completedTaskId = UUID.randomUUID();
		TaskPublicService taskPublicService = mock(TaskPublicService.class);

		when(taskPublicService.getRecentRoomTasks(roomId, 20))
				.thenReturn(List.of(
						task(activeTaskId, roomId, "진행 중 검토", TaskStatus.IN_PROGRESS),
						task(completedTaskId, roomId, "완료된 산출물 검수", TaskStatus.DONE)
				));

		var context = service(taskPublicService).retrieve(
				UUID.randomUUID(),
				roomId,
				"완료된 작업 기준으로 회고 정리해줘",
				"ko-KR",
				AgentCommandMode.ANSWER
		);

		assertThat(context.taskIds()).containsExactly(completedTaskId, activeTaskId);
		assertThat(context.promptBlock().indexOf("완료된 산출물 검수"))
				.isLessThan(context.promptBlock().indexOf("진행 중 검토"));
		assertThat(context.promptBlock()).contains("workState=COMPLETED");
	}

	@Test
	void wbsQuestionUsesRoomContextItems() {
		UUID roomId = UUID.randomUUID();
		UUID wbsItemId = UUID.randomUUID();
		WbsItemPublicService wbsItemPublicService = mock(WbsItemPublicService.class);

		when(wbsItemPublicService.getRoomContextItems(roomId, 20))
				.thenReturn(List.of(wbsItem(wbsItemId, roomId, "画面設計")));

		var context = service(wbsItemPublicService).retrieve(
				UUID.randomUUID(),
				roomId,
				"現在のWBSを教えて",
				"ja-JP",
				AgentCommandMode.ANSWER
		);

		verify(wbsItemPublicService).getRoomContextItems(roomId, 20);
		assertThat(context.grounded()).isTrue();
		assertThat(context.sourceTypes()).containsExactly(ProjectRoomGroundingSourceType.WBS);
		assertThat(context.wbsItemIds()).containsExactly(wbsItemId);
		assertThat(context.promptBlock()).contains("[WBS]").contains("画面設計");
	}

	@Test
	void completedWbsQuestionPrioritizesDoneItems() {
		UUID roomId = UUID.randomUUID();
		UUID activeWbsItemId = UUID.randomUUID();
		UUID completedWbsItemId = UUID.randomUUID();
		WbsItemPublicService wbsItemPublicService = mock(WbsItemPublicService.class);

		when(wbsItemPublicService.getRoomContextItems(roomId, 20))
				.thenReturn(List.of(
						wbsItem(activeWbsItemId, roomId, "구현 진행", WbsStatus.IN_PROGRESS),
						wbsItem(completedWbsItemId, roomId, "요구사항 정리 완료", WbsStatus.DONE)
				));

		var context = service(wbsItemPublicService).retrieve(
				UUID.randomUUID(),
				roomId,
				"끝난 WBS 기준으로 다음 단계 추천해줘",
				"ko-KR",
				AgentCommandMode.ANSWER
		);

		assertThat(context.wbsItemIds()).containsExactly(completedWbsItemId, activeWbsItemId);
		assertThat(context.promptBlock().indexOf("요구사항 정리 완료"))
				.isLessThan(context.promptBlock().indexOf("구현 진행"));
		assertThat(context.promptBlock()).contains("workState=COMPLETED");
	}

	@Test
	void scheduleQuestionUsesRoomSchedulesBetween() {
		UUID roomId = UUID.randomUUID();
		UUID scheduleId = UUID.randomUUID();
		SchedulePublicService schedulePublicService = mock(SchedulePublicService.class);

		when(schedulePublicService.getRoomSchedulesBetween(eq(roomId), any(Instant.class), any(Instant.class)))
				.thenReturn(List.of(schedule(scheduleId, roomId, "검수 회의")));

		var context = service(schedulePublicService).retrieve(
				UUID.randomUUID(),
				roomId,
				"이번 주 일정 알려줘",
				"ko-KR",
				AgentCommandMode.ANSWER
		);

		verify(schedulePublicService).getRoomSchedulesBetween(eq(roomId), any(Instant.class), any(Instant.class));
		assertThat(context.grounded()).isTrue();
		assertThat(context.sourceTypes()).containsExactly(ProjectRoomGroundingSourceType.SCHEDULE);
		assertThat(context.scheduleIds()).containsExactly(scheduleId);
		assertThat(context.promptBlock()).contains("[SCHEDULE]").contains("검수 회의");
	}

	@Test
	void agentSuggestionQuestionUsesRecentRoomSuggestions() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID suggestionId = UUID.randomUUID();
		AgentSuggestionPublicService agentSuggestionPublicService = mock(AgentSuggestionPublicService.class);

		when(agentSuggestionPublicService.getRecentRoomSuggestions(userId, roomId, 10))
				.thenReturn(List.of(suggestion(suggestionId, userId, roomId, AgentSuggestionType.TODO)));

		var context = service(agentSuggestionPublicService).retrieve(
				userId,
				roomId,
				"AI 후보함 보여줘",
				"ko-KR",
				AgentCommandMode.ANSWER
		);

		verify(agentSuggestionPublicService).getRecentRoomSuggestions(userId, roomId, 10);
		assertThat(context.grounded()).isTrue();
		assertThat(context.sourceTypes()).containsExactly(ProjectRoomGroundingSourceType.AGENT_SUGGESTION);
		assertThat(context.agentSuggestionIds()).containsExactly(suggestionId);
		assertThat(context.promptBlock()).contains("[AGENT_SUGGESTION]").contains("TODO");
	}

	@Test
	void multipleSourcesPreserveSourceTypesAndEvidenceIds() {
		UUID roomId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		UUID scheduleId = UUID.randomUUID();
		TaskPublicService taskPublicService = mock(TaskPublicService.class);
		SchedulePublicService schedulePublicService = mock(SchedulePublicService.class);

		when(taskPublicService.getRecentRoomTasks(roomId, 20))
				.thenReturn(List.of(task(taskId, roomId, "미완료 화면 구현")));
		when(schedulePublicService.getRoomSchedulesBetween(eq(roomId), any(Instant.class), any(Instant.class)))
				.thenReturn(List.of(schedule(scheduleId, roomId, "이번 주 배포")));

		var context = service(taskPublicService, schedulePublicService).retrieve(
				UUID.randomUUID(),
				roomId,
				"미완료 TODO와 일정 기준으로 다음 작업 추천해줘",
				"ko-KR",
				AgentCommandMode.SUGGEST
		);

		assertThat(context.grounded()).isTrue();
		assertThat(context.sourceTypes()).containsExactly(
				ProjectRoomGroundingSourceType.TASK,
				ProjectRoomGroundingSourceType.SCHEDULE
		);
		assertThat(context.taskIds()).containsExactly(taskId);
		assertThat(context.scheduleIds()).containsExactly(scheduleId);
		assertThat(context.promptBlock()).contains("[TASK]").contains("[SCHEDULE]");
	}

	@Test
	void returnsUngroundedWhenNoSourceDataExists() {
		TaskPublicService taskPublicService = mock(TaskPublicService.class);
		UUID roomId = UUID.randomUUID();

		when(taskPublicService.getRecentRoomTasks(roomId, 20)).thenReturn(List.of());

		var context = service(taskPublicService).retrieve(
				UUID.randomUUID(),
				roomId,
				"현재 미완료 TODO 알려줘",
				"ko-KR",
				AgentCommandMode.ANSWER
		);

		assertThat(context.grounded()).isFalse();
		assertThat(context.promptBlock()).isEmpty();
	}

	@Test
	void unknownQuestionDoesNotCallAnySourceService() {
		ResourceSemanticSearchPublicService searchService = mock(ResourceSemanticSearchPublicService.class);
		TaskPublicService taskPublicService = mock(TaskPublicService.class);

		var context = service(searchService, taskPublicService).retrieve(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"내 ID가 뭐야",
				"ko-KR",
				AgentCommandMode.ANSWER
		);

		verify(searchService, never()).search(any(), any(), any(), any(), anyInt());
		verify(taskPublicService, never()).getRecentRoomTasks(any(), anyInt());
		assertThat(context.grounded()).isFalse();
	}

	private ProjectRoomGroundingService service(ResourceSemanticSearchPublicService searchService) {
		return new ProjectRoomGroundingService(
				searchService,
				mock(ResourcePublicService.class),
				new AgentRagProperties(true, 5, 0.72D, 0.0D),
				mock(TaskPublicService.class),
				mock(WbsItemPublicService.class),
				mock(SchedulePublicService.class),
				mock(AgentSuggestionPublicService.class)
		);
	}

	private ProjectRoomGroundingService service(TaskPublicService taskPublicService) {
		return new ProjectRoomGroundingService(
				mock(ResourceSemanticSearchPublicService.class),
				mock(ResourcePublicService.class),
				new AgentRagProperties(true, 5, 0.72D, 0.0D),
				taskPublicService,
				mock(WbsItemPublicService.class),
				mock(SchedulePublicService.class),
				mock(AgentSuggestionPublicService.class)
		);
	}

	private ProjectRoomGroundingService service(WbsItemPublicService wbsItemPublicService) {
		return new ProjectRoomGroundingService(
				mock(ResourceSemanticSearchPublicService.class),
				mock(ResourcePublicService.class),
				new AgentRagProperties(true, 5, 0.72D, 0.0D),
				mock(TaskPublicService.class),
				wbsItemPublicService,
				mock(SchedulePublicService.class),
				mock(AgentSuggestionPublicService.class)
		);
	}

	private ProjectRoomGroundingService service(SchedulePublicService schedulePublicService) {
		return new ProjectRoomGroundingService(
				mock(ResourceSemanticSearchPublicService.class),
				mock(ResourcePublicService.class),
				new AgentRagProperties(true, 5, 0.72D, 0.0D),
				mock(TaskPublicService.class),
				mock(WbsItemPublicService.class),
				schedulePublicService,
				mock(AgentSuggestionPublicService.class)
		);
	}

	private ProjectRoomGroundingService service(AgentSuggestionPublicService agentSuggestionPublicService) {
		return new ProjectRoomGroundingService(
				mock(ResourceSemanticSearchPublicService.class),
				mock(ResourcePublicService.class),
				new AgentRagProperties(true, 5, 0.72D, 0.0D),
				mock(TaskPublicService.class),
				mock(WbsItemPublicService.class),
				mock(SchedulePublicService.class),
				agentSuggestionPublicService
		);
	}

	private ProjectRoomGroundingService service(
			TaskPublicService taskPublicService,
			SchedulePublicService schedulePublicService
	) {
		return new ProjectRoomGroundingService(
				mock(ResourceSemanticSearchPublicService.class),
				mock(ResourcePublicService.class),
				new AgentRagProperties(true, 5, 0.72D, 0.0D),
				taskPublicService,
				mock(WbsItemPublicService.class),
				schedulePublicService,
				mock(AgentSuggestionPublicService.class)
		);
	}

	private ProjectRoomGroundingService service(
			ResourceSemanticSearchPublicService searchService,
			TaskPublicService taskPublicService
	) {
		return new ProjectRoomGroundingService(
				searchService,
				mock(ResourcePublicService.class),
				new AgentRagProperties(true, 5, 0.72D, 0.0D),
				taskPublicService,
				mock(WbsItemPublicService.class),
				mock(SchedulePublicService.class),
				mock(AgentSuggestionPublicService.class)
		);
	}

	private ProjectRoomGroundingService service(
			ResourceSemanticSearchPublicService searchService,
			ResourcePublicService resourcePublicService
	) {
		return new ProjectRoomGroundingService(
				searchService,
				resourcePublicService,
				new AgentRagProperties(true, 5, 0.72D, 0.0D),
				mock(TaskPublicService.class),
				mock(WbsItemPublicService.class),
				mock(SchedulePublicService.class),
				mock(AgentSuggestionPublicService.class)
		);
	}

	private ResourceSearchHit hit(UUID resourceId, String chunkText, double similarityScore) {
		return new ResourceSearchHit(
				UUID.randomUUID(),
				resourceId,
				0,
				chunkText,
				2,
				10,
				12,
				120,
				260,
				"contract.pdf",
				"{\"pageNumber\":2,\"startLine\":10,\"endLine\":12,\"startOffset\":120,\"endOffset\":260,\"originalName\":\"contract.pdf\"}",
				similarityScore
		);
	}

	private ResourceResult resource(UUID resourceId, UUID userId, UUID roomId, String title) {
		return new ResourceResult(
				resourceId,
				userId,
				roomId,
				title,
				ResourceKind.FILE,
				ResourceVisibility.ROOM_SHARED,
				ResourceStatus.READY,
				Instant.parse("2026-07-01T01:00:00Z"),
				Instant.parse("2026-07-01T01:00:00Z")
		);
	}

	private ResourceSummaryResult resourceSummary(UUID resourceId, String summaryJson) {
		return new ResourceSummaryResult(
				UUID.randomUUID(),
				resourceId,
				UUID.randomUUID(),
				summaryJson,
				"[]",
				ResourceSummaryStatus.READY,
				"test-prompt",
				"analysis.v1",
				"test-model",
				Instant.parse("2026-07-01T01:00:00Z"),
				Instant.parse("2026-07-01T01:00:00Z")
		);
	}

	private TaskResult task(UUID taskId, UUID roomId, String title) {
		return task(taskId, roomId, title, TaskStatus.TODO);
	}

	private TaskResult task(UUID taskId, UUID roomId, String title, TaskStatus status) {
		return new TaskResult(
				taskId,
				UUID.randomUUID(),
				UUID.randomUUID(),
				roomId,
				null,
				title,
				"description",
				status,
				Instant.parse("2026-07-08T01:00:00Z"),
				Instant.parse("2026-07-01T01:00:00Z"),
				Instant.parse("2026-07-01T01:00:00Z")
		);
	}

	private WbsItemResult wbsItem(UUID wbsItemId, UUID roomId, String title) {
		return wbsItem(wbsItemId, roomId, title, WbsStatus.TODO);
	}

	private WbsItemResult wbsItem(UUID wbsItemId, UUID roomId, String title, WbsStatus status) {
		return new WbsItemResult(
				wbsItemId,
				roomId,
				null,
				title,
				1,
				status,
				Instant.parse("2026-07-01T01:00:00Z"),
				Instant.parse("2026-07-01T01:00:00Z")
		);
	}

	private ScheduleResult schedule(UUID scheduleId, UUID roomId, String title) {
		return new ScheduleResult(
				scheduleId,
				UUID.randomUUID(),
				roomId,
				null,
				null,
				null,
				null,
				null,
				title,
				Instant.parse("2026-07-07T01:00:00Z"),
				Instant.parse("2026-07-07T02:00:00Z"),
				false,
				ScheduleSyncStatus.LOCAL_ONLY,
				null,
				Instant.parse("2026-07-01T01:00:00Z"),
				Instant.parse("2026-07-01T01:00:00Z")
		);
	}

	private AgentSuggestionResponse suggestion(
			UUID suggestionId,
			UUID userId,
			UUID roomId,
			AgentSuggestionType suggestionType
	) {
		return new AgentSuggestionResponse(
				suggestionId,
				userId,
				roomId,
				null,
				null,
				suggestionType,
				AgentSuggestionStatus.DRAFT,
				Map.of("title", "화면 설계"),
				Map.of(),
				null,
				null,
				null,
				null,
				Instant.parse("2026-07-01T01:00:00Z"),
				Instant.parse("2026-07-01T01:00:00Z")
		);
	}
}
