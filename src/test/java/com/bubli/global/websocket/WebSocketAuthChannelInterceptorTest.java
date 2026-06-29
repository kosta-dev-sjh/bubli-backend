package com.bubli.global.websocket;

import com.bubli.global.security.AuthUser;
import com.bubli.global.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Principal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

class WebSocketAuthChannelInterceptorTest {

    private JwtTokenProvider jwtTokenProvider;
    private WebSocketSubscriptionAuthorizationPort subscriptionAuthorizationPort;
    private WebSocketAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secret", "test-secret-key-must-be-at-least-32-bytes");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpireMs", 3_600_000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpireMs", 1_209_600_000L);
        ReflectionTestUtils.invokeMethod(jwtTokenProvider, "init");

        subscriptionAuthorizationPort = Mockito.mock(WebSocketSubscriptionAuthorizationPort.class);
        interceptor = new WebSocketAuthChannelInterceptor(jwtTokenProvider, subscriptionAuthorizationPort);
    }

    @Test
    void connectWithBearerTokenSetsAuthenticatedUser() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.createAccessToken(new AuthUser(userId));

        Message<byte[]> result = preSend(connectMessage("Bearer " + token));
        Principal principal = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class).getUser();

        assertThat(principal).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) principal;
        assertThat(authentication.getPrincipal()).isEqualTo(new AuthUser(userId));
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void connectWithoutBearerTokenIsRejected() {
        Message<byte[]> message = connectMessage(null);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("access token is required");
    }

    @Test
    void connectWithInvalidTokenIsRejected() {
        Message<byte[]> message = connectMessage("Bearer invalid-token");

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("Invalid WebSocket access token");
    }

    @Test
    void subscribeWithAuthenticatedUserChecksDestinationAuthorization() {
        UUID userId = UUID.randomUUID();
        AuthUser authUser = new AuthUser(userId);

        preSend(subscribeMessage(
                "/topic/project-rooms/%s/events".formatted(UUID.randomUUID()),
                authUser
        ));

        verify(subscriptionAuthorizationPort).authorize(
                org.mockito.ArgumentMatchers.eq(authUser),
                org.mockito.ArgumentMatchers.startsWith("/topic/project-rooms/")
        );
    }

    @Test
    void subscribeWithoutAuthenticatedUserIsRejected() {
        Message<byte[]> message = subscribeMessage("/user/queue/notifications", null);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("requires authentication");
    }

    @SuppressWarnings("unchecked")
    private Message<byte[]> preSend(Message<byte[]> message) {
        return (Message<byte[]>) interceptor.preSend(message, null);
    }

    private Message<byte[]> connectMessage(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> subscribeMessage(String destination, AuthUser authUser) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        if (authUser != null) {
            accessor.setUser(new UsernamePasswordAuthenticationToken(authUser, null));
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
