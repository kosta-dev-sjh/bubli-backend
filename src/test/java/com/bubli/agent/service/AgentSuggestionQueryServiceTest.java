package com.bubli.agent.service;

import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.repository.AgentSuggestionRepository;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.project.service.ProjectMembershipPublicService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentSuggestionQueryServiceTest {

    @Test
    void filtersMySuggestionsByStatusAndType() {
        UUID userId = UUID.randomUUID();
        AgentSuggestion task = suggestion(userId, AgentSuggestionType.TASK);
        AgentSuggestion reviewItem = suggestion(userId, AgentSuggestionType.REVIEW_ITEM);
        reviewItem.hold(UUID.randomUUID());
        AgentSuggestionRepository repository = mock(AgentSuggestionRepository.class);
        when(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(task, reviewItem));

        var responses = new AgentSuggestionQueryService(repository, mock(ProjectMembershipPublicService.class))
                .findMine(userId, AgentSuggestionStatus.DRAFT, AgentSuggestionType.TASK);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).suggestionType()).isEqualTo(AgentSuggestionType.TASK);
        assertThat(responses.get(0).status()).isEqualTo(AgentSuggestionStatus.DRAFT);
    }

    @Test
    void verifiesRoomMembershipBeforeFindingRoomSuggestions() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        AgentSuggestionRepository repository = mock(AgentSuggestionRepository.class);
        ProjectMembershipPublicService membershipService = mock(ProjectMembershipPublicService.class);
        when(repository.findAllByRoomIdOrderByCreatedAtDesc(roomId)).thenReturn(List.of());

        new AgentSuggestionQueryService(repository, membershipService)
                .findRoomSuggestions(userId, roomId, null, null);

        verify(membershipService).assertActiveMember(userId, roomId);
    }

    @Test
    void findsRoomConfirmationItemsByConfirmationTypes() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        AgentSuggestion question = suggestion(userId, AgentSuggestionType.QUESTION);
        AgentSuggestion contractField = suggestion(userId, AgentSuggestionType.CONTRACT_FIELD);
        AgentSuggestionRepository repository = mock(AgentSuggestionRepository.class);
        ProjectMembershipPublicService membershipService = mock(ProjectMembershipPublicService.class);
        when(repository.findAllByRoomIdAndSuggestionTypeInAndStatusOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(roomId),
                org.mockito.ArgumentMatchers.anyCollection(),
                org.mockito.ArgumentMatchers.eq(AgentSuggestionStatus.DRAFT)
        )).thenReturn(List.of(question, contractField));

        var responses = new AgentSuggestionQueryService(repository, membershipService)
                .findRoomConfirmationItems(userId, roomId, AgentSuggestionStatus.DRAFT);

        assertThat(responses)
                .extracting(response -> response.suggestionType())
                .containsExactly(AgentSuggestionType.QUESTION, AgentSuggestionType.CONTRACT_FIELD);
        verify(membershipService).assertActiveMember(userId, roomId);
    }

    private AgentSuggestion suggestion(UUID userId, AgentSuggestionType type) {
        return AgentSuggestion.draft(
                userId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                type,
                Map.of("title", type.name()),
                null
        );
    }
}
