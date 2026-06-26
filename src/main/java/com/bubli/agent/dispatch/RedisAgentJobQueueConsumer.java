package com.bubli.agent.dispatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agent.dispatch.adapter", havingValue = "redis")
public class RedisAgentJobQueueConsumer implements AgentJobQueueConsumerPort {

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	@Value("${agent.dispatch.redis.queue-key:bubli:agent-jobs}")
	private String queueKey;

	@Override
	public Optional<AgentJobQueueMessage> poll() {
		String payload = redisTemplate.opsForList().leftPop(queueKey);
		if (payload == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(payload, AgentJobQueueMessage.class));
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to deserialize agent job queue message.", exception);
		}
	}
}
