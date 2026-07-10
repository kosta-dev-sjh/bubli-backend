package com.bubli.global.websocket;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DesktopPresenceRegistryTest {

	private final DesktopPresenceRegistry registry = new DesktopPresenceRegistry();

	@Test
	void isActiveIsFalseForUnknownUser() {
		assertThat(registry.isActive(UUID.randomUUID())).isFalse();
	}

	@Test
	void registerMakesUserActive() {
		UUID userId = UUID.randomUUID();

		registry.register(userId, "session-1");

		assertThat(registry.isActive(userId)).isTrue();
	}

	@Test
	void userStaysActiveWhileAnySessionRemains() {
		UUID userId = UUID.randomUUID();
		registry.register(userId, "session-1");
		registry.register(userId, "session-2");

		registry.unregister("session-1");

		assertThat(registry.isActive(userId)).isTrue();
	}

	@Test
	void userBecomesInactiveOnceLastSessionUnregisters() {
		UUID userId = UUID.randomUUID();
		registry.register(userId, "session-1");
		registry.register(userId, "session-2");

		registry.unregister("session-1");
		registry.unregister("session-2");

		assertThat(registry.isActive(userId)).isFalse();
	}

	@Test
	void unregisteringUnknownSessionIsNoOp() {
		registry.unregister("never-registered");

		assertThat(registry.isActive(UUID.randomUUID())).isFalse();
	}
}
