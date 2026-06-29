package com.bubli.websocket.service;

import com.bubli.chat.service.ChatRoomAccessPublicService;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.security.AuthUser;
import com.bubli.project.service.ProjectMembershipPublicService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class WebSocketSubscriptionAuthorizationServiceTest {

    private final ChatRoomAccessPublicService chatRoomAccessPublicService = mock(ChatRoomAccessPublicService.class);
    private final ProjectMembershipPublicService projectMembershipPublicService = mock(ProjectMembershipPublicService.class);
    private final WebSocketSubscriptionAuthorizationService service = new WebSocketSubscriptionAuthorizationService(
            chatRoomAccessPublicService,
            projectMembershipPublicService
    );

    @Test
    void authorizesChatTopicWithChatRoomMembership() {
        UUID userId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();

        service.authorize(new AuthUser(userId), "/topic/chat/" + chatRoomId);

        verify(chatRoomAccessPublicService).assertActiveMember(userId, chatRoomId);
        verifyNoInteractions(projectMembershipPublicService);
    }

    @Test
    void authorizesProjectRoomEventsTopicWithRoomMembership() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        service.authorize(new AuthUser(userId), "/topic/project-rooms/%s/events".formatted(roomId));

        verify(projectMembershipPublicService).assertActiveMember(userId, roomId);
        verifyNoInteractions(chatRoomAccessPublicService);
    }

    @Test
    void authorizesPersonalNotificationQueueForAuthenticatedUser() {
        service.authorize(new AuthUser(UUID.randomUUID()), "/user/queue/notifications");

        verifyNoInteractions(chatRoomAccessPublicService, projectMembershipPublicService);
    }

    @Test
    void rejectsUnsupportedDestination() {
        assertThatThrownBy(() -> service.authorize(new AuthUser(UUID.randomUUID()), "/topic/unknown"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_403_001));
    }
}
