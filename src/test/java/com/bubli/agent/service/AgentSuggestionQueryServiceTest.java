package com.bubli.agent.service;

import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.repository.AgentSuggestionRepository;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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

        var responses = new AgentSuggestionQueryService(repository)
                .findMine(userId, AgentSuggestionStatus.DRAFT, AgentSuggestionType.TASK);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).suggestionType()).isEqualTo(AgentSuggestionType.TASK);
        assertThat(responses.get(0).status()).isEqualTo(AgentSuggestionStatus.DRAFT);
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
