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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentJobExecutionResultRecorder {

    static final String SUCCEEDED_EVENT_TYPE = "SUCCEEDED";
    static final String FAILED_EVENT_TYPE = "FAILED";
    static final String SUCCEEDED_EVENT_MESSAGE = "에이전트 작업 실행이 완료되었습니다.";
    static final String DEFAULT_FAILURE_MESSAGE = "에이전트 작업 실행이 실패했습니다.";
    static final String SUCCEEDED_NOTIFICATION_TITLE = "AI 작업이 완료되었습니다.";
    static final String FAILED_NOTIFICATION_TITLE = "AI 작업이 실패했습니다.";

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
                .orElseGet(() -> {
                    log.warn("Skipped marking agent job succeeded because job is missing or not RUNNING. jobId={}", jobId);
                    return false;
                });
    }

    @Transactional
    public boolean recordFailed(UUID jobId, String errorCode, String errorMessage) {
        return agentJobRepository.findById(jobId)
                .filter(agentJob -> agentJob.getStatus() == AgentJobStatus.RUNNING)
                .map(agentJob -> markFailed(agentJob, errorCode, errorMessage))
                .orElseGet(() -> {
                    log.warn(
                            "Skipped marking agent job failed because job is missing or not RUNNING. jobId={}, errorCode={}, errorMessage={}",
                            jobId,
                            errorCode,
                            truncate(errorMessage)
                    );
                    return false;
                });
    }

    private boolean markSucceeded(AgentJob agentJob) {
        Locale locale = locale(agentJob.getRequestedByUserId());
        String message = message("agent.job.succeeded.event", locale, SUCCEEDED_EVENT_MESSAGE);
        log.info(
                "Marking agent job succeeded. jobId={}, jobType={}, roomId={}, resourceId={}, retryCount={}",
                agentJob.getId(),
                agentJob.getJobType(),
                agentJob.getRoomId(),
                agentJob.getResourceId(),
                agentJob.getRetryCount()
        );
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
        log.warn(
                "Marking agent job failed. jobId={}, jobType={}, roomId={}, resourceId={}, retryCountBefore={}, errorCode={}, errorMessage={}",
                agentJob.getId(),
                agentJob.getJobType(),
                agentJob.getRoomId(),
                agentJob.getResourceId(),
                agentJob.getRetryCount(),
                errorCode,
                truncate(message)
        );
        agentJob.markFailed(errorCode, message);
        log.warn(
                "Agent job failed state saved. jobId={}, jobType={}, status={}, retryCountAfter={}, errorCode={}, errorMessage={}",
                agentJob.getId(),
                agentJob.getJobType(),
                agentJob.getStatus(),
                agentJob.getRetryCount(),
                agentJob.getErrorCode(),
                truncate(agentJob.getErrorMessage())
        );
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

    private String truncate(String value) {
        if (value == null || value.length() <= 300) {
            return value;
        }
        return value.substring(0, 300) + "...";
    }
}
