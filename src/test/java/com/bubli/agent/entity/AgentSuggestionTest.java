package com.bubli.agent.entity;

import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentSuggestionTest {

    @Test
    void preservesOriginalContentWhenReviewerModifiesSuggestion() {
        AgentSuggestion suggestion = createSuggestion();
        UUID reviewerId = UUID.randomUUID();

        suggestion.modify(reviewerId, Map.of("amount", "3200000"));

        assertThat(suggestion.getOriginalContentJson()).containsEntry("amount", "3000000");
        assertThat(suggestion.getContentJson()).containsEntry("amount", "3200000");
        assertThat(suggestion.getStatus()).isEqualTo(AgentSuggestionStatus.DRAFT);
        assertThat(suggestion.getReviewedBy()).isEqualTo(reviewerId);
    }

    @Test
    void jsonContentAllowsExplicitNullValues() {
        Map<String, Object> content = new HashMap<>();
        content.put("amount", null);

        AgentSuggestion suggestion = AgentSuggestion.pending(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                AgentSuggestionType.REVIEW_ITEM,
                "금액 확인 필요",
                content,
                null
        );

        assertThat(suggestion.getContentJson()).containsEntry("amount", null);
    }

    @Test
    void connectsEvidenceAndPreventsChangesAfterApproval() {
        AgentSuggestion suggestion = createSuggestion();
        suggestion.addEvidence(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "contract.pdf",
                3,
                "계약 금액은 삼백만원이다.",
                new BigDecimal("0.9123456")
        );
        suggestion.approve(UUID.randomUUID());

        assertThat(suggestion.getEvidences()).hasSize(1);
        assertThat(suggestion.getStatus()).isEqualTo(AgentSuggestionStatus.APPROVED);
        assertThatThrownBy(() -> suggestion.hold(UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
    }

    private AgentSuggestion createSuggestion() {
        return AgentSuggestion.pending(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                AgentSuggestionType.REVIEW_ITEM,
                "계약 금액 확인",
                Map.of("amount", "3000000"),
                new BigDecimal("0.8000")
        );
    }
}
