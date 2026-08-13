package com.bubli.agent.dispatch;

import com.bubli.agent.type.AgentJobType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"rawtypes", "unchecked"})
class RedisAgentJobQueueConsumerTest {

	@Test
	void claimReceiptUsesRedisSequenceSoIdenticalPayloadsRemainIndependent() {
		RedisScript<?> claimScript = (RedisScript<?>) ReflectionTestUtils.getField(
				RedisAgentJobQueueConsumer.class, "CLAIM_SCRIPT");

		assertThat(claimScript).isNotNull();
		assertThat(claimScript.getScriptAsString())
				.contains("redis.call('INCR', KEYS[7])")
				.contains("payload .. ':' .. tostring(sequence)")
				.contains("redis.call('HSET', KEYS[5], receipt, payload)")
				.contains("return cjson.encode({payload = payload, receipt = receipt, attempt = 1})");
		assertThat(claimScript.getResultType()).isEqualTo(String.class);
	}

	@Test
	void claimReturnsEmptyWhenRedisHasNoReadyMessage() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		RedisAgentJobQueueConsumer consumer = consumer(redisTemplate, mock(ObjectMapper.class));
		doReturn(null).when(redisTemplate).execute(any(RedisScript.class), anyList(), any());

		assertThat(consumer.claim()).isEmpty();

		verify(redisTemplate).execute(any(RedisScript.class), anyList(), any());
	}

	@Test
	void claimReturnsDeliveryWithReceiptAndAttempt() throws Exception {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
		RedisAgentJobQueueConsumer consumer = consumer(redisTemplate, objectMapper);
		AgentJobQueueMessage message = message();
		String payload = objectMapper.writeValueAsString(message);
		String result = objectMapper.writeValueAsString(Map.of(
				"payload", payload,
				"receipt", "receipt-1",
				"attempt", 2
		));
		doReturn(result)
				.when(redisTemplate).execute(any(RedisScript.class), anyList(), any());

		Optional<AgentJobQueueDelivery> claimed = consumer.claim();

		assertThat(claimed).hasValueSatisfying(delivery -> {
			assertThat(delivery.message()).isEqualTo(message);
			assertThat(delivery.receipt()).isEqualTo("receipt-1");
			assertThat(delivery.rawPayload()).isEqualTo(payload);
			assertThat(delivery.deliveryAttempt()).isEqualTo(2);
		});
	}

	@Test
	void malformedClaimEnvelopeFailsWithoutIndexError() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		RedisAgentJobQueueConsumer consumer = consumer(
				redisTemplate,
				new ObjectMapper().findAndRegisterModules()
		);
		doReturn("[\"payload-only\"]")
				.when(redisTemplate).execute(any(RedisScript.class), anyList(), any());

		assertThatThrownBy(consumer::claim)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Failed to deserialize Redis agent job claim.")
				.hasCauseInstanceOf(JsonProcessingException.class);
	}

	@Test
	void acknowledgeRemovesClaimedPayload() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		RedisAgentJobQueueConsumer consumer = consumer(redisTemplate, mock(ObjectMapper.class));
		AgentJobQueueDelivery delivery = new AgentJobQueueDelivery(message(), "receipt-1", "payload", 1);

		consumer.acknowledge(delivery);

		verify(redisTemplate).execute(any(RedisScript.class), anyList(), any());
	}

	@Test
	void recoverStaleReturnsRestoredCount() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		RedisAgentJobQueueConsumer consumer = consumer(redisTemplate, mock(ObjectMapper.class));
		doReturn(3L).when(redisTemplate).execute(any(RedisScript.class), anyList(), any(), any());

		assertThat(consumer.recoverStale(Duration.ofMinutes(30), 10)).isEqualTo(3);
	}

	@Test
	void malformedClaimIsMovedToDeadLetter() throws Exception {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
		RedisAgentJobQueueConsumer consumer = consumer(redisTemplate, objectMapper);
		String result = objectMapper.writeValueAsString(Map.of(
				"payload", "bad json",
				"receipt", "receipt-1",
				"attempt", 1
		));
		doReturn(result)
				.doReturn(1L)
				.when(redisTemplate).execute(any(RedisScript.class), anyList(), any());

		assertThatThrownBy(consumer::claim)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Failed to deserialize agent job queue message.");

		verify(redisTemplate, times(2)).execute(any(RedisScript.class), anyList(), any());
	}

	@Test
	void invalidClaimWithReceiptIsMovedToDeadLetter() throws Exception {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
		RedisAgentJobQueueConsumer consumer = consumer(redisTemplate, objectMapper);
		String result = objectMapper.writeValueAsString(Map.of(
				"payload", "",
				"receipt", "receipt-1",
				"attempt", 1
		));
		doReturn(result)
				.doReturn(1L)
				.when(redisTemplate).execute(any(RedisScript.class), anyList(), any());

		assertThatThrownBy(consumer::claim)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Redis agent job claim has an invalid envelope.");

		verify(redisTemplate, times(2)).execute(any(RedisScript.class), anyList(), any());
	}

	private RedisAgentJobQueueConsumer consumer(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
		RedisAgentJobQueueConsumer consumer = new RedisAgentJobQueueConsumer(redisTemplate, objectMapper);
		ReflectionTestUtils.setField(consumer, "queueKey", "agent-jobs:test");
		return consumer;
	}

	private AgentJobQueueMessage message() {
		return new AgentJobQueueMessage(
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE, Instant.now());
	}
}
