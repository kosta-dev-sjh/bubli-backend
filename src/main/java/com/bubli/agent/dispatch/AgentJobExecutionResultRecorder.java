package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.entity.AgentJobEvent;
import com.bubli.agent.repository.AgentJobEventRepository;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.global.locale.SupportedLocale;
import com.bubli.personal.notification.service.NotificationPublicService;
import com.bubli.personal.notification.type.NotificationSourceType;
import com.bubli.project.service.ProjectRoomEventPublicService;
import com.bubli.user.service.UserLocalePublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentJobExecutionResultRecorder {

	static final String SUCCEEDED_EVENT_TYPE = "SUCCEEDED";
	static final String FAILED_EVENT_TYPE = "FAILED";
	static final String SUCCEEDED_EVENT_MESSAGE = "?먯씠?꾪듃 ?묒뾽 ?ㅽ뻾???꾨즺?섏뿀?듬땲??";
	static final String DEFAULT_FAILURE_MESSAGE = "?먯씠?꾪듃 ?묒뾽 ?ㅽ뻾???ㅽ뙣?덉뒿?덈떎.";
	static final String SUCCEEDED_NOTIFICATION_TITLE = "AI ?묒뾽???꾨즺?섏뿀?듬땲??";
	static final String FAILED_NOTIFICATION_TITLE = "AI ?묒뾽???ㅽ뙣?덉뒿?덈떎.";

	private final AgentJobRepository agentJobRepository;
	private final AgentJobEventRepository agentJobEventRepository;
	private final NotificationPublicService notificationPublicService;
	private final ProjectRoomEventPublicService projectRoomEventPublicService;
	private final MessageSource messageSource;
	private final UserLocalePublicService userLocalePublicService;

	@Transactional
	public boolean recordSucceeded(UUID jobId) {
		return agentJobRepository.findById(jobId)
				.filter(agentJob -> agentJob.getStatus() == AgentJobStatus.RUNNING)
				.map(this::markSucceeded)
				.orElse(false);
	}

	@Transactional
	public boolean recordFailed(UUID jobId, String errorCode, String errorMessage) {
		return agentJobRepository.findById(jobId)
				.filter(agentJob -> agentJob.getStatus() == AgentJobStatus.RUNNING)
				.map(agentJob -> markFailed(agentJob, errorCode, errorMessage))
				.orElse(false);
	}

	private boolean markSucceeded(AgentJob agentJob) {
		Locale locale = locale(agentJob.getRequestedByUserId());
		String message = message("agent.job.succeeded.event", locale, SUCCEEDED_EVENT_MESSAGE);
		agentJob.markSucceeded();
		agentJobEventRepository.save(AgentJobEvent.create(
				agentJob.getId(),
				SUCCEEDED_EVENT_TYPE,
				message
		));
		notificationPublicService.create(
				agentJob.getRequestedByUserId(),
				NotificationSourceType.AGENT,
				agentJob.getId(),
				message("agent.job.succeeded.notification.title", locale, SUCCEEDED_NOTIFICATION_TITLE),
				notificationBody(agentJob, message, locale)
		);
		recordProjectRoomEvent(agentJob, "SUCCEEDED", message);
		return true;
	}

	private boolean markFailed(AgentJob agentJob, String errorCode, String errorMessage) {
		Locale locale = locale(agentJob.getRequestedByUserId());
		String message = failureMessage(errorMessage, locale);
		agentJob.markFailed(errorCode, message);
		agentJobEventRepository.save(AgentJobEvent.create(
				agentJob.getId(),
				FAILED_EVENT_TYPE,
				message
		));
		notificationPublicService.create(
				agentJob.getRequestedByUserId(),
				NotificationSourceType.AGENT,
				agentJob.getId(),
				message("agent.job.failed.notification.title", locale, FAILED_NOTIFICATION_TITLE),
				notificationBody(agentJob, message, locale)
		);
		recordProjectRoomEvent(agentJob, "FAILED", message);
		return true;
	}

	private void recordProjectRoomEvent(AgentJob agentJob, String status, String message) {
		if (agentJob.getRoomId() == null) {
			return;
		}
		projectRoomEventPublicService.recordAgentJobCompleted(
				agentJob.getRequestedByUserId(),
				agentJob.getRoomId(),
				agentJob.getId(),
				agentJob.getJobType().name(),
				status,
				message
		);
	}

	private String failureMessage(String errorMessage, Locale locale) {
		if (errorMessage == null || errorMessage.isBlank()) {
			return message("agent.job.failed.event", locale, DEFAULT_FAILURE_MESSAGE);
		}
		return errorMessage;
	}

	private String notificationBody(AgentJob agentJob, String message, Locale locale) {
		return messageSource.getMessage(
				"agent.job.notification.body",
				new Object[]{agentJob.getJobType(), agentJob.getId(), message},
				"jobType=%s, jobId=%s, message=%s".formatted(
						agentJob.getJobType(),
						agentJob.getId(),
						message
				),
				locale
		);
	}

	private Locale locale(UUID userId) {
		return SupportedLocale.resolve(userLocalePublicService.resolveLocaleCode(userId, null)).toJavaLocale();
	}

	private String message(String key, Locale locale, String defaultMessage) {
		return messageSource.getMessage(key, null, defaultMessage, locale);
	}
}
