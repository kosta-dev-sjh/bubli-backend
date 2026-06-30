package com.bubli.agent.service;

import com.bubli.agent.dispatch.AgentJobQueueMessage;
import com.bubli.agent.type.AgentJobType;
import com.bubli.chat.service.ChatMessagePublicService;
import com.bubli.memory.service.RoomMemoryPublicService;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.resource.service.ResourcePublicService;
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
import java.util.List;
import java.util.Map;
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
        AgentJobContextCollector collector = new AgentJobContextCollector(
                mock(ProjectMembershipPublicService.class),
                mock(ResourcePublicService.class),
                taskPublicService,
                mock(WbsItemPublicService.class),
                schedulePublicService,
                mock(ChatMessagePublicService.class),
                mock(RoomMemoryPublicService.class)
        );
        when(taskPublicService.getDueBetweenTasks(eq(userId), any(), any())).thenReturn(List.of(
                task("완료 작업", TaskStatus.DONE),
                task("잔여 작업", TaskStatus.IN_PROGRESS)
        ));
        when(schedulePublicService.getSchedulesBetween(eq(userId), any(), any())).thenReturn(List.of(
                schedule("오늘 회의")
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
                "오늘 회의"
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
}
