package com.bubli.agent.service;

import com.bubli.agent.contract.v1.AgentAnalysisResult;
import com.bubli.agent.contract.v1.Analysis;
import com.bubli.agent.contract.v1.ModelInfo;
import com.bubli.agent.contract.v1.Suggestion;
import com.bubli.agent.contract.v1.SuggestionType;
import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.repository.AgentSuggestionRepository;
import com.bubli.agent.type.AgentSuggestionReviewAction;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentSuggestionCommandServiceTest {

    @Test
    void reviewsDraftSuggestion() {
        UUID suggestionId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        AgentSuggestion suggestion = suggestion(suggestionId);
        AgentSuggestionRepository repository = mock(AgentSuggestionRepository.class);
        when(repository.findById(suggestionId)).thenReturn(Optional.of(suggestion));

        var response = new AgentSuggestionCommandService(repository)
                .review(suggestionId, reviewerId, AgentSuggestionReviewAction.APPROVE, null);

        assertThat(response.status()).isEqualTo(AgentSuggestionStatus.APPROVED);
        assertThat(response.reviewedBy()).isEqualTo(reviewerId);
    }

    @Test
    void mapsInvalidReviewTransitionToBadRequest() {
        UUID suggestionId = UUID.randomUUID();
        AgentSuggestion suggestion = suggestion(suggestionId);
        suggestion.approve(UUID.randomUUID());
        AgentSuggestionRepository repository = mock(AgentSuggestionRepository.class);
        when(repository.findById(suggestionId)).thenReturn(Optional.of(suggestion));

        assertThatThrownBy(() -> new AgentSuggestionCommandService(repository)
                .review(suggestionId, UUID.randomUUID(), AgentSuggestionReviewAction.REJECT, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.COMMON_400_002));
    }

    @Test
    void createsDraftSuggestionsFromAnalysisResult() {
        AgentSuggestionRepository repository = mock(AgentSuggestionRepository.class);
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        UUID resourceId = UUID.randomUUID();

        List<com.bubli.agent.dto.AgentSuggestionResponse> responses = new AgentSuggestionCommandService(repository)
                .createDrafts(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        resourceId,
                        analysisResult(resourceId)
                );

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).suggestionType()).isEqualTo(AgentSuggestionType.TASK);
        assertThat(responses.get(0).payloadJson()).containsEntry("title", "로그인 구현");
        assertThat(responses.get(0).evidenceJson()).containsEntry("resourceId", resourceId.toString());
        assertThat(responses.get(1).suggestionType()).isEqualTo(AgentSuggestionType.REVIEW_ITEM);
    }

    private AgentSuggestion suggestion(UUID suggestionId) {
        AgentSuggestion suggestion = AgentSuggestion.draft(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                AgentSuggestionType.REVIEW_ITEM,
                Map.of("amount", "3000000"),
                Map.of("page", 3)
        );
        ReflectionTestUtils.setField(suggestion, "id", suggestionId);
        return suggestion;
    }

    private AgentAnalysisResult analysisResult(UUID resourceId) {
        return new AgentAnalysisResult(
                AgentAnalysisResult.SCHEMA_VERSION,
                resourceId,
                new ModelInfo("test-model", "prompt-v1"),
                new Analysis("summary", List.of("login"), List.of(), List.of()),
                List.of(
                        new Suggestion(
                                SuggestionType.TASK,
                                "로그인 구현",
                                "로그인 API를 구현한다.",
                                "로그인 기능 필요",
                                0.9,
                                null,
                                null
                        ),
                        new Suggestion(
                                SuggestionType.CONTRACT_FIELD,
                                null,
                                null,
                                "계약 금액 300만원",
                                0.8,
                                "amount",
                                "3000000"
                        )
                )
        );
    }
}
