package com.bubli.global.websocket;

import com.bubli.global.security.AuthUser;

public interface WebSocketSubscriptionAuthorizationPort {

    void authorize(AuthUser authUser, String destination);
}
