package com.bubli.agent.service;

import com.bubli.agent.repository.AgentModelCallLogRepository;
import com.bubli.agent.type.AgentJobType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentModelUsageGuard {

	private static final String USER_DAILY_LIMIT_ERROR = "AI_DAILY_USAGE_LIMIT_EXCEEDED";
	private static final String JOB_TYPE_DAILY_LIMIT_ERROR = "AI_JOB_TYPE_DAILY_USAGE_LIMIT_EXCEEDED";

	private final AgentModelCallLogRepository agentModelCallLogRepository;

	@Value("${agent.model-usage.user-daily-limit:0}")
	private int userDailyLimit;

	@Value("${agent.model-usage.job-type-daily-limit:0}")
	private int jobTypeDailyLimit;

	@Transactional(readOnly = true)
	public void assertWithinDailyLimit(UUID userId, AgentJobType jobType) {
		Instant since = Instant.now().minus(1, ChronoUnit.DAYS);
		if (userDailyLimit > 0) {
			long userCalls = agentModelCallLogRepository.countByUserSince(userId, since);
			if (userCalls >= userDailyLimit) {
				throw new AgentModelUsageLimitExceededException(
						USER_DAILY_LIMIT_ERROR,
						"AI 모델 하루 사용량 제한을 초과했습니다."
				);
			}
		}
		if (jobTypeDailyLimit > 0) {
			long jobTypeCalls = agentModelCallLogRepository.countByUserAndJobTypeSince(userId, jobType, since);
			if (jobTypeCalls >= jobTypeDailyLimit) {
				throw new AgentModelUsageLimitExceededException(
						JOB_TYPE_DAILY_LIMIT_ERROR,
						"해당 AI 기능의 하루 사용량 제한을 초과했습니다."
				);
			}
		}
	}
}
