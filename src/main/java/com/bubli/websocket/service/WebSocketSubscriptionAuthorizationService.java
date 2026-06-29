package com.bubli.websocket.service;

import com.bubli.chat.service.ChatRoomAccessPublicService;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.security.AuthUser;
import com.bubli.global.websocket.WebSocketSubscriptionAuthorizationPort;
import com.bubli.project.service.ProjectMembershipPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class WebSocketSubscriptionAuthorizationService implements WebSocketSubscriptionAuthorizationPort {

    private static final Pattern CHAT_TOPIC = Pattern.compile("^/topic/chat/([0-9a-fA-F-]{36})$");
    private static final Pattern PROJECT_ROOM_EVENTS_TOPIC =
            Pattern.compile("^/topic/project-rooms/([0-9a-fA-F-]{36})/events$");
    private static final String USER_NOTIFICATION_QUEUE = "/user/queue/notifications";

    private final ChatRoomAccessPublicService chatRoomAccessPublicService;
    private final ProjectMembershipPublicService projectMembershipPublicService;

    @Override
    public void authorize(AuthUser authUser, String destination) {
        if (authUser == null || destination == null || destination.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_403_001);
        }

        Matcher chatMatcher = CHAT_TOPIC.matcher(destination);
        if (chatMatcher.matches()) {
            chatRoomAccessPublicService.assertActiveMember(authUser.userId(), parseUuid(chatMatcher.group(1)));
            return;
        }

        Matcher projectRoomEventsMatcher = PROJECT_ROOM_EVENTS_TOPIC.matcher(destination);
        if (projectRoomEventsMatcher.matches()) {
            projectMembershipPublicService.assertActiveMember(authUser.userId(), parseUuid(projectRoomEventsMatcher.group(1)));
            return;
        }

        if (USER_NOTIFICATION_QUEUE.equals(destination)) {
            return;
        }

        throw new BusinessException(ErrorCode.AUTH_403_001);
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.AUTH_403_001);
        }
    }
}
