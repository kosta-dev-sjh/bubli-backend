package com.bubli.personal.dashboard.service;

import com.bubli.personal.dashboard.dto.DashboardWorkResponse;
import com.bubli.personal.notification.service.NotificationPublicService;
import com.bubli.personal.timer.dto.TimeLogResult;
import com.bubli.personal.timer.service.TimeLogPublicService;
import com.bubli.work.schedule.service.SchedulePublicService;
import com.bubli.work.task.service.TaskPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

	private final TaskPublicService taskPublicService;
	private final SchedulePublicService schedulePublicService;
	private final NotificationPublicService notificationPublicService;
	private final TimeLogPublicService timeLogPublicService;

	@Transactional(readOnly = true)
	public DashboardWorkResponse getWorkDashboard(UUID userId) {
		Instant now = Instant.now();
		Instant startOfToday = now.truncatedTo(ChronoUnit.DAYS);
		Instant startOfTomorrow = startOfToday.plus(1, ChronoUnit.DAYS);
		Instant endOfWeek = startOfToday.plus(7, ChronoUnit.DAYS);

		TimeLogResult runningTimer = timeLogPublicService.getRunningTimer(userId).orElse(null);

		return new DashboardWorkResponse(
				taskPublicService.getDueBetweenTasks(userId, startOfToday, startOfTomorrow),
				taskPublicService.getDueBetweenTasks(userId, startOfTomorrow, endOfWeek),
				schedulePublicService.getSchedulesBetween(userId, startOfToday, startOfTomorrow),
				notificationPublicService.countUnread(userId),
				runningTimer,
				List.of()
		);
	}
}
