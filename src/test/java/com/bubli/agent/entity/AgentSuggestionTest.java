package com.bubli.agent.entity;

import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentSuggestionTest {

    @Test
    void storesPayloadAndEvidenceJsonSeparately() {
        Map<String, Object> evidence = Map.of(
                "resourceId", UUID.randomUUID().toString(),
                "page", 3
        );

        AgentSuggestion suggestion = AgentSuggestion.draft(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                AgentSuggestionType.REVIEW_ITEM,
                Map.of("amount", "3000000"),
                evidence
        );

        assertThat(suggestion.getPayloadJson()).containsEntry("amount", "3000000");
        assertThat(suggestion.getEvidenceJson()).containsEntry("page", 3);
        assertThat(suggestion.getStatus()).isEqualTo(AgentSuggestionStatus.DRAFT);
    }

    @Test
    void payloadAllowsExplicitNullValues() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", null);

        AgentSuggestion suggestion = AgentSuggestion.draft(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                AgentSuggestionType.REVIEW_ITEM,
                payload,
                null
        );

        assertThat(suggestion.getPayloadJson()).containsEntry("amount", null);
    }

    @Test
    void preventsChangesAfterApproval() {
        AgentSuggestion suggestion = createSuggestion();

        suggestion.approve(UUID.randomUUID());

        assertThat(suggestion.getStatus()).isEqualTo(AgentSuggestionStatus.APPROVED);
        assertThatThrownBy(() -> suggestion.hold(UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void modifiesDraftPayload() {
        AgentSuggestion suggestion = createSuggestion();
        UUID reviewerId = UUID.randomUUID();

        suggestion.modify(reviewerId, Map.of("amount", "3200000"));

        assertThat(suggestion.getPayloadJson()).containsEntry("amount", "3200000");
        assertThat(suggestion.getReviewedBy()).isEqualTo(reviewerId);
    }

    private AgentSuggestion createSuggestion() {
        return AgentSuggestion.draft(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                AgentSuggestionType.REVIEW_ITEM,
                Map.of("amount", "3000000"),
                Map.of("page", 3)
        );
    }
}
