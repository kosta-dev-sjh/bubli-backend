package com.bubli.widget.dto;

import com.bubli.personal.timer.dto.TimeLogResult;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.task.dto.TaskResult;

import java.util.List;

public record WidgetSummaryResponse(
        WidgetContextResponse context,
        List<WidgetBubbleSettingResponse> bubbles,
        List<TaskResult> tasks,
        List<ScheduleResult> schedules,
        long unreadNotificationCount,
        TimeLogResult runningTimer,
        List<String> agentSuggestionSummary
) {}
