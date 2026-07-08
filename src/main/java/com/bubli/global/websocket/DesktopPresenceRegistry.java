package com.bubli.global.websocket;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// 유저별로 지금 데스크톱(Tauri) 앱 세션이 하나라도 붙어 있는지만 추적한다. 인스턴스가 1대라
// 인메모리로 충분하다 — 여러 대로 늘리면 Redis Set(TTL+하트비트 갱신)으로 옮겨야 한다.
@Component
public class DesktopPresenceRegistry {

	private final Map<UUID, Set<String>> sessionIdsByUserId = new ConcurrentHashMap<>();
	private final Map<String, UUID> userIdBySessionId = new ConcurrentHashMap<>();

	public void register(UUID userId, String sessionId) {
		sessionIdsByUserId.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(sessionId);
		userIdBySessionId.put(sessionId, userId);
	}

	public void unregister(String sessionId) {
		UUID userId = userIdBySessionId.remove(sessionId);
		if (userId == null) {
			return;
		}
		sessionIdsByUserId.computeIfPresent(userId, (key, sessionIds) -> {
			sessionIds.remove(sessionId);
			return sessionIds.isEmpty() ? null : sessionIds;
		});
	}

	public boolean isActive(UUID userId) {
		Set<String> sessionIds = sessionIdsByUserId.get(userId);
		return sessionIds != null && !sessionIds.isEmpty();
	}
}
