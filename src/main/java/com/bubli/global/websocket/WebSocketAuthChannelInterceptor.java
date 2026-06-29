package com.bubli.global.websocket;

import com.bubli.global.error.BusinessException;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final WebSocketSubscriptionAuthorizationPort subscriptionAuthorizationPort;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !accessor.isMutable()) {
            accessor = StompHeaderAccessor.wrap(message);
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor));
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    private Principal authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new MessagingException("WebSocket access token is required.");
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        try {
            AuthUser authUser = jwtTokenProvider.getAuthUser(token);
            return new UsernamePasswordAuthenticationToken(
                    authUser,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
        } catch (JwtException | IllegalArgumentException e) {
            throw new MessagingException("Invalid WebSocket access token.", e);
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        AuthUser authUser = currentAuthUser(accessor);
        try {
            subscriptionAuthorizationPort.authorize(authUser, accessor.getDestination());
        } catch (BusinessException e) {
            throw new MessagingException("WebSocket subscription is not allowed.", e);
        }
    }

    private AuthUser currentAuthUser(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user instanceof UsernamePasswordAuthenticationToken authentication
                && authentication.getPrincipal() instanceof AuthUser authUser) {
            return authUser;
        }
        throw new MessagingException("WebSocket subscription requires authentication.");
    }
}
