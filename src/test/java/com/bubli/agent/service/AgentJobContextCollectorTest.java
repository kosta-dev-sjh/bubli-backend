package com.bubli.agent.service;

import com.bubli.agent.dispatch.AgentJobQueueMessage;
import com.bubli.agent.type.AgentJobType;
import com.bubli.activity.dto.ActivityLogResult;
import com.bubli.activity.service.ActivityPublicService;
import com.bubli.chat.service.ChatMessagePublicService;
import com.bubli.memory.service.RoomMemoryPublicService;
import com.bubli.personal.memo.dto.MemoResult;
import com.bubli.personal.memo.service.MemoPublicService;
import com.bubli.personal.memo.type.MemoStatus;
import com.bubli.personal.notification.dto.NotificationResponse;
import com.bubli.personal.notification.service.NotificationPublicService;
import com.bubli.personal.notification.type.NotificationSourceType;
import com.bubli.personal.notification.type.NotificationStatus;
import com.bubli.personal.timer.dto.TimeLogResult;
import com.bubli.personal.timer.service.TimeLogPublicService;
import com.bubli.personal.timer.type.TimeLogStatus;
import com.bubli.personal.timer.type.TimerType;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.widget.dto.WidgetTodaySummaryResponse;
import com.bubli.widget.service.WidgetPublicService;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.service.SchedulePublicService;
import com.bubli.work.schedule.type.ScheduleSyncStatus;
import com.bubli.work.task.dto.TaskResult;
import com.bubli.work.task.service.TaskPublicService;
import com.bubli.work.task.type.TaskStatus;
import com.bubli.work.wbs.service.WbsItemPublicService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentJobContextCollectorTest {

    @Test
    void dailySummaryContextUsesRequestedTimezoneAndSplitsDoneAndRemainingTasks() {
        UUID userId = UUID.randomUUID();
        TaskPublicService taskPublicService = mock(TaskPublicService.class);
        SchedulePublicService schedulePublicService = mock(SchedulePublicService.class);
        MemoPublicService memoPublicService = mock(MemoPublicService.class);
        ActivityPublicService activityPublicService = mock(ActivityPublicService.class);
        NotificationPublicService notificationPublicService = mock(NotificationPublicService.class);
        TimeLogPublicService timeLogPublicService = mock(TimeLogPublicService.class);
        WidgetPublicService widgetPublicService = mock(WidgetPublicService.class);
        AgentSuggestionPublicService agentSuggestionPublicService = mock(AgentSuggestionPublicService.class);
        AgentJobContextCollector collector = new AgentJobContextCollector(
                mock(ProjectMembershipPublicService.class),
                mock(ResourcePublicService.class),
                taskPublicService,
                mock(WbsItemPublicService.class),
                schedulePublicService,
                mock(ChatMessagePublicService.class),
                mock(RoomMemoryPublicService.class),
                memoPublicService,
                activityPublicService,
                notificationPublicService,
                timeLogPublicService,
                widgetPublicService,
                agentSuggestionPublicService
        );
        when(taskPublicService.getDueBetweenTasks(eq(userId), any(), any())).thenReturn(List.of(
                task("완료 작업", TaskStatus.DONE),
                task("잔여 작업", TaskStatus.IN_PROGRESS)
        ));
        when(schedulePublicService.getSchedulesBetween(eq(userId), any(), any())).thenReturn(List.of(
                schedule("오늘 회의")
        ));
        when(memoPublicService.getUpdatedMemosBetween(eq(userId), any(), any(), eq(20))).thenReturn(List.of(
                memo("클라이언트 피드백 확인")
        ));
        when(activityPublicService.getActivityContextBetween(eq(userId), any(), any(), eq(20))).thenReturn(List.of(
                activity("IntelliJ IDEA", "Bubli Backend", 1800L)
        ));
        when(notificationPublicService.getNotificationsBetween(eq(userId), any(), any(), eq(10))).thenReturn(List.of(
                notification("AI 제안 검토 필요")
        ));
        when(timeLogPublicService.getRunningTimer(userId)).thenReturn(Optional.of(runningTimer()));
        when(widgetPublicService.getUsageSummary(eq(userId), eq(LocalDate.of(2026, 7, 1))))
                .thenReturn(new WidgetTodaySummaryResponse(LocalDate.of(2026, 7, 1), 3, 7, 900L, List.of()));
        when(agentSuggestionPublicService.getReviewRequiredSummaries(userId, 10)).thenReturn(List.of(
                "DOCUMENT_DRAFT: 회의록 초안"
        ));

        var context = collector.collect(new AgentJobQueueMessage(
                UUID.randomUUID(),
                userId,
                null,
                null,
                AgentJobType.DAILY_SUMMARY,
                Map.of("summaryDate", "2026-07-01", "timezone", "Asia/Tokyo"),
                Instant.now()
        ));

        assertThat(context.promptBlock()).contains(
                "summaryDate=2026-07-01 timezone=Asia/Tokyo",
                "[Daily summary completed tasks]",
                "완료 작업",
                "[Daily summary remaining tasks]",
                "잔여 작업",
                "[Daily summary today schedules]",
                "오늘 회의",
                "[Daily summary memos]",
                "클라이언트 피드백 확인",
                "[Daily summary activity]",
                "IntelliJ IDEA",
                "Bubli Backend",
                "[Daily summary notifications]",
                "AI 제안 검토 필요",
                "[Daily summary running timer]",
                "WORK",
                "[Daily summary widget usage]",
                "visibleSeconds=900",
                "[Daily summary pending agent suggestions]",
                "DOCUMENT_DRAFT: 회의록 초안"
        );
        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(taskPublicService).getDueBetweenTasks(eq(userId), fromCaptor.capture(), toCaptor.capture());
        assertThat(fromCaptor.getValue()).isEqualTo(Instant.parse("2026-06-30T15:00:00Z"));
        assertThat(toCaptor.getValue()).isEqualTo(Instant.parse("2026-07-01T15:00:00Z"));
    }

    private TaskResult task(String title, TaskStatus status) {
        Instant now = Instant.parse("2026-07-01T01:00:00Z");
        return new TaskResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                title,
                null,
                status,
                now,
                now,
                now
        );
    }

    private ScheduleResult schedule(String title) {
        Instant startsAt = Instant.parse("2026-07-01T02:00:00Z");
        Instant endsAt = Instant.parse("2026-07-01T03:00:00Z");
        return new ScheduleResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                title,
                startsAt,
                endsAt,
                false,
                ScheduleSyncStatus.LOCAL_ONLY,
                null,
                startsAt,
                startsAt
        );
    }

    private MemoResult memo(String body) {
        Instant now = Instant.parse("2026-07-01T04:00:00Z");
        return new MemoResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                body,
                MemoStatus.ACTIVE,
                now,
                now
        );
    }

    private ActivityLogResult activity(String appName, String windowTitle, Long durationSeconds) {
        Instant now = Instant.parse("2026-07-01T05:00:00Z");
        return new ActivityLogResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                appName,
                windowTitle,
                now,
                now.plusSeconds(durationSeconds),
                durationSeconds,
                now
        );
    }

    private NotificationResponse notification(String title) {
        Instant now = Instant.parse("2026-07-01T06:00:00Z");
        return new NotificationResponse(
                UUID.randomUUID(),
                NotificationSourceType.AGENT,
                UUID.randomUUID(),
                title,
                "본문",
                NotificationStatus.UNREAD,
                null,
                now
        );
    }

    private TimeLogResult runningTimer() {
        Instant now = Instant.parse("2026-07-01T07:00:00Z");
        return new TimeLogResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                TimerType.WORK,
                "timer-key",
                null,
                TimeLogStatus.RUNNING,
                now,
                now,
                null,
                null,
                now,
                now,
                now
        );
    }
}
