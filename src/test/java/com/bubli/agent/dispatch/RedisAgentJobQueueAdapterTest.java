package com.bubli.agent.dispatch;

import com.bubli.agent.type.AgentJobType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisAgentJobQueueAdapterTest {

	@Test
	void dispatchPushesSerializedQueueMessageToConfiguredRedisList() throws Exception {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		ObjectMapper objectMapper = mock(ObjectMapper.class);
		@SuppressWarnings("unchecked")
		ListOperations<String, String> listOperations = mock(ListOperations.class);
		RedisAgentJobQueueAdapter adapter = new RedisAgentJobQueueAdapter(redisTemplate, objectMapper);
		ReflectionTestUtils.setField(adapter, "queueKey", "agent-jobs:test");
		AgentJobDispatchCommand command = command();
		when(redisTemplate.opsForList()).thenReturn(listOperations);
		when(objectMapper.writeValueAsString(any(AgentJobQueueMessage.class))).thenReturn("{\"jobId\":\"test\"}");

		adapter.dispatch(command);

		verify(redisTemplate).opsForList();
		verify(listOperations).rightPush("agent-jobs:test", "{\"jobId\":\"test\"}");
		verify(objectMapper).writeValueAsString(any(AgentJobQueueMessage.class));
	}

	@Test
	void dispatchThrowsWhenQueueMessageSerializationFails() throws Exception {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		ObjectMapper objectMapper = mock(ObjectMapper.class);
		RedisAgentJobQueueAdapter adapter = new RedisAgentJobQueueAdapter(redisTemplate, objectMapper);
		ReflectionTestUtils.setField(adapter, "queueKey", "agent-jobs:test");
		AgentJobDispatchCommand command = command();
		when(objectMapper.writeValueAsString(any(AgentJobQueueMessage.class)))
				.thenThrow(new JsonProcessingException("bad json") {
				});

		assertThatThrownBy(() -> adapter.dispatch(command))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Failed to serialize agent job queue message.");
	}

	private AgentJobDispatchCommand command() {
		return new AgentJobDispatchCommand(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE
		);
	}
}
