package com.bubli.agent.service;

import com.bubli.agent.dto.AgentJobContext;
import com.bubli.agent.dto.AgentSuggestionResponse;
import com.bubli.agent.type.AgentCommandMode;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.chat.dto.ChatMessageResult;
import com.bubli.chat.service.ChatMessagePublicService;
import com.bubli.chat.type.MessageType;
import com.bubli.memory.dto.RoomMemorySummaryContextResult;
import com.bubli.memory.service.RoomMemoryPublicService;
import com.bubli.memory.type.SummaryStatus;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.project.service.ProjectRoomEventPublicService;
import com.bubli.user.service.UserLocalePublicService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectRoomAgentCommandServiceTest {

    @Test
    void suggestCommandCreatesDraftSuggestionAndIncludesSuggestionIdInResponseBody() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        AgentSuggestionCommandService suggestionCommandService = mock(AgentSuggestionCommandService.class);
        ChatMessagePublicService chatMessagePublicService = mock(ChatMessagePublicService.class);
        RoomMemoryPublicService memoryPublicService = mock(RoomMemoryPublicService.class);
        ProjectRoomEventPublicService eventPublicService = mock(ProjectRoomEventPublicService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        when(suggestionCommandService.createDraft(
                eq(userId),
                eq(roomId),
                eq(null),
                eq(resourceId),
                eq(AgentSuggestionType.REVIEW_ITEM),
                any(),
                any()
        )).thenReturn(suggestionResponse(suggestionId, userId, roomId, resourceId, AgentSuggestionType.REVIEW_ITEM));
        when(chatMessagePublicService.createRoomAgentResponse(eq(userId), eq(roomId), any(), eq(resourceId)))
                .thenAnswer(invocation -> chatMessage(invocation.getArgument(2), resourceId));
        when(memoryPublicService.createDraft(eq(userId), eq(roomId), eq(10L), eq(10L), any()))
                .thenReturn(new RoomMemorySummaryContextResult(
                        UUID.randomUUID(),
                        10L,
                        10L,
                        "{}",
                        SummaryStatus.DRAFT,
                        Instant.now()
                ));

        var response = service(
                chatMessagePublicService,
                memoryPublicService,
                suggestionCommandService,
                eventPublicService,
                objectMapper
        ).execute(
                userId,
                roomId,
                "review contract risk",
                AgentCommandMode.SUGGEST,
                List.of(resourceId)
        );

        assertThat(response.suggestions()).hasSize(1);
        assertThat(response.suggestions().getFirst().suggestionId()).isEqualTo(suggestionId);
        assertThat(response.message().body().get("suggestionIds").get(0).asText()).isEqualTo(suggestionId.toString());
        verify(eventPublicService).recordAgentSuggestionsCreated(
                userId,
                roomId,
                List.of(suggestionId),
                List.of(AgentSuggestionType.REVIEW_ITEM.name())
        );
    }

    @Test
    void answerCommandDoesNotCreateSuggestion() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        AgentSuggestionCommandService suggestionCommandService = mock(AgentSuggestionCommandService.class);
        ChatMessagePublicService chatMessagePublicService = mock(ChatMessagePublicService.class);
        RoomMemoryPublicService memoryPublicService = mock(RoomMemoryPublicService.class);
        ProjectRoomEventPublicService eventPublicService = mock(ProjectRoomEventPublicService.class);

        when(chatMessagePublicService.createRoomAgentResponse(eq(userId), eq(roomId), any(), eq(null)))
                .thenAnswer(invocation -> chatMessage(invocation.getArgument(2), null));
        when(memoryPublicService.createDraft(eq(userId), eq(roomId), eq(10L), eq(10L), any()))
                .thenReturn(new RoomMemorySummaryContextResult(
                        UUID.randomUUID(),
                        10L,
                        10L,
                        "{}",
                        SummaryStatus.DRAFT,
                        Instant.now()
                ));

        var response = service(
                chatMessagePublicService,
                memoryPublicService,
                suggestionCommandService,
                eventPublicService,
                new ObjectMapper()
        ).execute(
                userId,
                roomId,
                "answer this",
                AgentCommandMode.ANSWER,
                List.of()
        );

        assertThat(response.suggestions()).isEmpty();
        verify(suggestionCommandService, never()).createDraft(any(), any(), any(), any(), any(), any(), any());
        verify(eventPublicService, never()).recordAgentSuggestionsCreated(any(), any(), any(), any());
    }

    @Test
    void fallbackAnswerUsesResolvedUserLocale() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        AgentSuggestionCommandService suggestionCommandService = mock(AgentSuggestionCommandService.class);
        ChatMessagePublicService chatMessagePublicService = mock(ChatMessagePublicService.class);
        RoomMemoryPublicService memoryPublicService = mock(RoomMemoryPublicService.class);
        ProjectRoomEventPublicService eventPublicService = mock(ProjectRoomEventPublicService.class);

        when(chatMessagePublicService.createRoomAgentResponse(eq(userId), eq(roomId), any(), eq(null)))
                .thenAnswer(invocation -> chatMessage(invocation.getArgument(2), null));
        when(memoryPublicService.createDraft(eq(userId), eq(roomId), eq(10L), eq(10L), any()))
                .thenReturn(new RoomMemorySummaryContextResult(
                        UUID.randomUUID(),
                        10L,
                        10L,
                        "{}",
                        SummaryStatus.DRAFT,
                        Instant.now()
                ));

        var response = service(
                chatMessagePublicService,
                memoryPublicService,
                suggestionCommandService,
                eventPublicService,
                new ObjectMapper(),
                "en-US"
        ).execute(
                userId,
                roomId,
                "answer this",
                AgentCommandMode.ANSWER,
                List.of()
        );

        assertThat(response.message().body().get("text").asText())
                .contains("I checked the currently collected project context.");
    }

    private ProjectRoomAgentCommandService service(
            ChatMessagePublicService chatMessagePublicService,
            RoomMemoryPublicService memoryPublicService,
            AgentSuggestionCommandService suggestionCommandService,
            ProjectRoomEventPublicService eventPublicService,
            ObjectMapper objectMapper
    ) {
        return service(
                chatMessagePublicService,
                memoryPublicService,
                suggestionCommandService,
                eventPublicService,
                objectMapper,
                "ko-KR"
        );
    }

    @SuppressWarnings("unchecked")
    private ProjectRoomAgentCommandService service(
            ChatMessagePublicService chatMessagePublicService,
            RoomMemoryPublicService memoryPublicService,
            AgentSuggestionCommandService suggestionCommandService,
            ProjectRoomEventPublicService eventPublicService,
            ObjectMapper objectMapper,
            String locale
    ) {
        AgentJobContextCollector contextCollector = mock(AgentJobContextCollector.class);
        when(contextCollector.collect(any())).thenReturn(new AgentJobContext("context", 7));
        ObjectProvider<ChatModel> chatModelProvider = mock(ObjectProvider.class);
        when(chatModelProvider.getIfAvailable()).thenReturn(null);
        UserLocalePublicService userLocalePublicService = mock(UserLocalePublicService.class);
        when(userLocalePublicService.resolveLocaleCode(any(UUID.class), any())).thenReturn(locale);
        return new ProjectRoomAgentCommandService(
                mock(ProjectMembershipPublicService.class),
                contextCollector,
                chatMessagePublicService,
                memoryPublicService,
                suggestionCommandService,
                eventPublicService,
                userLocalePublicService,
                chatModelProvider,
                mock(ObjectProvider.class),
                objectMapper
        );
    }

    private AgentSuggestionResponse suggestionResponse(
            UUID suggestionId,
            UUID userId,
            UUID roomId,
            UUID resourceId,
            AgentSuggestionType suggestionType
    ) {
        return new AgentSuggestionResponse(
                suggestionId,
                userId,
                roomId,
                null,
                resourceId,
                suggestionType,
                AgentSuggestionStatus.DRAFT,
                Map.of("title", "review contract risk"),
                Map.of("source", "PROJECT_ROOM_AGENT_COMMAND"),
                null,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    private ChatMessageResult chatMessage(JsonNode body, UUID resourceId) {
        return new ChatMessageResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "AGENT",
                null,
                "Bubli Agent",
                null,
                10L,
                MessageType.AGENT_RESPONSE,
                body,
                resourceId,
                Instant.now()
        );
    }
}
