package com.bubli.agent.dispatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agent.dispatch.adapter", havingValue = "redis")
public class RedisAgentJobQueueAdapter implements AgentJobDispatchPort {

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	@Value("${agent.dispatch.redis.queue-key:bubli:agent-jobs}")
	private String queueKey;

	@Override
	public void dispatch(AgentJobDispatchCommand command) {
		AgentJobQueueMessage message = AgentJobQueueMessage.from(command, Instant.now());
		try {
			redisTemplate.opsForList().rightPush(queueKey, objectMapper.writeValueAsString(message));
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize agent job queue message.", exception);
		}
	}
}
