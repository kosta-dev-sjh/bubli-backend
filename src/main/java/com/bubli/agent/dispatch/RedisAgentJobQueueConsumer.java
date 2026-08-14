package com.bubli.agent.dispatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agent.dispatch.adapter", havingValue = "redis")
public class RedisAgentJobQueueConsumer implements AgentJobQueueConsumerPort {

	/*
	 * Reclaimed receipts are preferred. Fresh claims atomically move the payload
	 * into the processing list and attach a unique receipt. The sequence avoids
	 * collisions even when two queue entries contain byte-for-byte identical JSON.
	 */
	private static final DefaultRedisScript<String> CLAIM_SCRIPT = new DefaultRedisScript<>("""
			local receipt = redis.call('LPOP', KEYS[6])
			while receipt do
			  local reclaimedPayload = redis.call('HGET', KEYS[5], receipt)
			  if reclaimedPayload then
			    local reclaimedAttempt = redis.call('HINCRBY', KEYS[4], receipt, 1)
			    redis.call('ZADD', KEYS[3], ARGV[1], receipt)
			    return cjson.encode({
			      payload = reclaimedPayload,
			      receipt = receipt,
			      attempt = reclaimedAttempt
			    })
			  end
			  receipt = redis.call('LPOP', KEYS[6])
			end

			local payload = redis.call('LMOVE', KEYS[1], KEYS[2], 'LEFT', 'RIGHT')
			if not payload then
			  return nil
			end
			local sequence = redis.call('INCR', KEYS[7])
			receipt = redis.sha1hex(payload .. ':' .. tostring(sequence))
			redis.call('HSET', KEYS[5], receipt, payload)
			redis.call('HSET', KEYS[4], receipt, 1)
			redis.call('ZADD', KEYS[3], ARGV[1], receipt)
			return cjson.encode({payload = payload, receipt = receipt, attempt = 1})
			""", String.class);

	private static final DefaultRedisScript<Long> ACK_SCRIPT = new DefaultRedisScript<>("""
			local payload = redis.call('HGET', KEYS[4], ARGV[1])
			local removed = 0
			if payload then
			  removed = redis.call('LREM', KEYS[1], 1, payload)
			end
			redis.call('ZREM', KEYS[2], ARGV[1])
			redis.call('HDEL', KEYS[3], ARGV[1])
			redis.call('HDEL', KEYS[4], ARGV[1])
			redis.call('LREM', KEYS[5], 1, ARGV[1])
			return removed
			""", Long.class);

	/*
	 * Recovery only transfers receipt ownership. The payload remains in processing,
	 * so a crash during recovery cannot create a remove-then-push loss window.
	 */
	private static final DefaultRedisScript<Long> RECLAIM_SCRIPT = new DefaultRedisScript<>("""
			local receipts = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1], 'LIMIT', 0, ARGV[2])
			local reclaimed = 0
			for _, receipt in ipairs(receipts) do
			  redis.call('ZREM', KEYS[1], receipt)
			  redis.call('RPUSH', KEYS[2], receipt)
			  reclaimed = reclaimed + 1
			end
			return reclaimed
			""", Long.class);

	private static final DefaultRedisScript<Long> DEAD_LETTER_SCRIPT = new DefaultRedisScript<>("""
			local payload = redis.call('HGET', KEYS[4], ARGV[1])
			local removed = 0
			if payload then
			  removed = redis.call('LREM', KEYS[1], 1, payload)
			  redis.call('RPUSH', KEYS[6], payload)
			end
			redis.call('ZREM', KEYS[2], ARGV[1])
			redis.call('HDEL', KEYS[3], ARGV[1])
			redis.call('HDEL', KEYS[4], ARGV[1])
			redis.call('LREM', KEYS[5], 1, ARGV[1])
			return removed
			""", Long.class);

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	@Value("${agent.dispatch.redis.queue-key:bubli:agent-jobs}")
	private String queueKey;

	@Override
	public Optional<AgentJobQueueDelivery> claim() {
		String result = redisTemplate.execute(
				CLAIM_SCRIPT,
				List.of(
						queueKey, processingKey(), claimsKey(), attemptsKey(), payloadsKey(),
						reclaimableKey(), receiptSequenceKey()),
				Long.toString(Instant.now().toEpochMilli())
		);
		if (result == null || result.isBlank()) {
			return Optional.empty();
		}
		RedisClaim claim = deserializeClaim(result);
		try {
			AgentJobQueueMessage message = objectMapper.readValue(claim.payload(), AgentJobQueueMessage.class);
			return Optional.of(new AgentJobQueueDelivery(
					message,
					claim.receipt(),
					claim.payload(),
					claim.attempt()
			));
		} catch (JsonProcessingException exception) {
			moveToDeadLetter(claim.receipt());
			throw new IllegalStateException("Failed to deserialize agent job queue message.", exception);
		}
	}

	private RedisClaim deserializeClaim(String result) {
		RedisClaim claim;
		try {
			claim = objectMapper.readValue(result, RedisClaim.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to deserialize Redis agent job claim.", exception);
		}
		if (claim == null
				|| claim.payload() == null || claim.payload().isBlank()
				|| claim.receipt() == null || claim.receipt().isBlank()
				|| claim.attempt() < 1) {
			if (claim != null && claim.receipt() != null && !claim.receipt().isBlank()) {
				moveToDeadLetter(claim.receipt());
			}
			throw new IllegalStateException("Redis agent job claim has an invalid envelope.");
		}
		return claim;
	}

	@Override
	public void acknowledge(AgentJobQueueDelivery delivery) {
		if (delivery.rawPayload() == null) {
			return;
		}
		redisTemplate.execute(
				ACK_SCRIPT,
				List.of(processingKey(), claimsKey(), attemptsKey(), payloadsKey(), reclaimableKey()),
				delivery.receipt()
		);
	}

	@Override
	public int recoverStale(Duration claimTimeout, int limit) {
		if (claimTimeout == null || claimTimeout.isNegative() || claimTimeout.isZero() || limit <= 0) {
			return 0;
		}
		long cutoff = Instant.now().minus(claimTimeout).toEpochMilli();
		Long reclaimed = redisTemplate.execute(
				RECLAIM_SCRIPT,
				List.of(claimsKey(), reclaimableKey()),
				Long.toString(cutoff),
				Integer.toString(limit)
		);
		return reclaimed == null ? 0 : reclaimed.intValue();
	}

	private void moveToDeadLetter(String receipt) {
		redisTemplate.execute(
				DEAD_LETTER_SCRIPT,
				List.of(
						processingKey(), claimsKey(), attemptsKey(), payloadsKey(),
						reclaimableKey(), deadLetterKey()),
				receipt
		);
	}

	private String processingKey() {
		return queueKey + ":processing";
	}

	private String claimsKey() {
		return queueKey + ":processing:claims";
	}

	private String attemptsKey() {
		return queueKey + ":processing:attempts";
	}

	private String payloadsKey() {
		return queueKey + ":processing:payloads";
	}

	private String reclaimableKey() {
		return queueKey + ":processing:reclaimable";
	}

	private String receiptSequenceKey() {
		return queueKey + ":processing:receipt-sequence";
	}

	private String deadLetterKey() {
		return queueKey + ":dead-letter";
	}

	private record RedisClaim(String payload, String receipt, long attempt) {
	}
}
