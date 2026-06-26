package com.bubli.agent.dispatch;

import com.bubli.agent.type.AgentJobType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisAgentJobQueueConsumerTest {

	@Test
	void pollReturnsEmptyWhenRedisListHasNoMessage() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		ObjectMapper objectMapper = mock(ObjectMapper.class);
		@SuppressWarnings("unchecked")
		ListOperations<String, String> listOperations = mock(ListOperations.class);
		RedisAgentJobQueueConsumer consumer = new RedisAgentJobQueueConsumer(redisTemplate, objectMapper);
		ReflectionTestUtils.setField(consumer, "queueKey", "agent-jobs:test");
		when(redisTemplate.opsForList()).thenReturn(listOperations);
		when(listOperations.leftPop("agent-jobs:test")).thenReturn(null);

		assertThat(consumer.poll()).isEmpty();

		verify(listOperations).leftPop("agent-jobs:test");
	}

	@Test
	void pollReturnsDeserializedQueueMessage() throws Exception {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		ObjectMapper objectMapper = mock(ObjectMapper.class);
		@SuppressWarnings("unchecked")
		ListOperations<String, String> listOperations = mock(ListOperations.class);
		RedisAgentJobQueueConsumer consumer = new RedisAgentJobQueueConsumer(redisTemplate, objectMapper);
		ReflectionTestUtils.setField(consumer, "queueKey", "agent-jobs:test");
		AgentJobQueueMessage message = message();
		when(redisTemplate.opsForList()).thenReturn(listOperations);
		when(listOperations.leftPop("agent-jobs:test")).thenReturn("{\"jobId\":\"test\"}");
		when(objectMapper.readValue("{\"jobId\":\"test\"}", AgentJobQueueMessage.class)).thenReturn(message);

		assertThat(consumer.poll()).hasValue(message);

		verify(listOperations).leftPop("agent-jobs:test");
		verify(objectMapper).readValue("{\"jobId\":\"test\"}", AgentJobQueueMessage.class);
	}

	@Test
	void pollThrowsWhenQueueMessageDeserializationFails() throws Exception {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		ObjectMapper objectMapper = mock(ObjectMapper.class);
		@SuppressWarnings("unchecked")
		ListOperations<String, String> listOperations = mock(ListOperations.class);
		RedisAgentJobQueueConsumer consumer = new RedisAgentJobQueueConsumer(redisTemplate, objectMapper);
		ReflectionTestUtils.setField(consumer, "queueKey", "agent-jobs:test");
		when(redisTemplate.opsForList()).thenReturn(listOperations);
		when(listOperations.leftPop("agent-jobs:test")).thenReturn("bad json");
		when(objectMapper.readValue("bad json", AgentJobQueueMessage.class))
				.thenThrow(new JsonProcessingException("bad json") {
				});

		assertThatThrownBy(consumer::poll)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Failed to deserialize agent job queue message.");
	}

	private AgentJobQueueMessage message() {
		return new AgentJobQueueMessage(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE,
				Instant.now()
		);
	}
}
