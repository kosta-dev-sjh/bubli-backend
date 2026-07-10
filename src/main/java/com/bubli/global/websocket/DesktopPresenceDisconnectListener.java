package com.bubli.global.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

// STOMP 세션이 끊기면(정상 종료든 네트워크 끊김이든) DesktopPresenceRegistry에서 정리한다.
@Component
@RequiredArgsConstructor
public class DesktopPresenceDisconnectListener {

	private final DesktopPresenceRegistry desktopPresenceRegistry;

	@EventListener
	public void onSessionDisconnect(SessionDisconnectEvent event) {
		desktopPresenceRegistry.unregister(event.getSessionId());
	}
}
