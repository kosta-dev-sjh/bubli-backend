package com.bubli.agent.service;

import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.repository.AgentSuggestionRepository;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.project.service.ProjectMembershipPublicService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Test
    void findsApprovedRoomRequirementsAsConfirmedRequirements() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        AgentSuggestion requirement = suggestion(userId, AgentSuggestionType.REQUIREMENT);
        AgentSuggestionRepository repository = mock(AgentSuggestionRepository.class);
        ProjectMembershipPublicService membershipService = mock(ProjectMembershipPublicService.class);
        when(repository.findAllByRoomIdAndSuggestionTypeAndStatus(
                roomId,
                AgentSuggestionType.REQUIREMENT,
                AgentSuggestionStatus.APPROVED
        )).thenReturn(List.of(requirement));

        var responses = new AgentSuggestionQueryService(repository, membershipService)
                .findRoomConfirmedRequirements(userId, roomId);

        assertThat(responses)
                .extracting(response -> response.suggestionType())
                .containsExactly(AgentSuggestionType.REQUIREMENT);
        verify(membershipService).assertActiveMember(userId, roomId);
    }

    @Test
    void findsApprovedContractReferencesByContractTypes() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        AgentSuggestion contractField = suggestion(userId, AgentSuggestionType.CONTRACT_FIELD);
        AgentSuggestion contractReview = suggestion(userId, AgentSuggestionType.CONTRACT_REVIEW);
        AgentSuggestionRepository repository = mock(AgentSuggestionRepository.class);
        ProjectMembershipPublicService membershipService = mock(ProjectMembershipPublicService.class);
        when(repository.findAllByRoomIdAndSuggestionTypeInAndStatusOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(roomId),
                org.mockito.ArgumentMatchers.anyCollection(),
                org.mockito.ArgumentMatchers.eq(AgentSuggestionStatus.APPROVED)
        )).thenReturn(List.of(contractField, contractReview));

        var responses = new AgentSuggestionQueryService(repository, membershipService)
                .findRoomContractReferences(userId, roomId);

        assertThat(responses)
                .extracting(response -> response.suggestionType())
                .containsExactly(AgentSuggestionType.CONTRACT_FIELD, AgentSuggestionType.CONTRACT_REVIEW);
        verify(membershipService).assertActiveMember(userId, roomId);
    }

    @Test
    void documentDraftResponseIncludesExportUrls() {
        UUID userId = UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        AgentSuggestion draft = suggestion(userId, AgentSuggestionType.DOCUMENT_DRAFT);
        ReflectionTestUtils.setField(draft, "id", suggestionId);

        var response = com.bubli.agent.dto.AgentSuggestionResponse.from(draft);

        assertThat(response.downloadUrl()).isEqualTo("/api/agent/suggestions/%s/export".formatted(suggestionId));
        assertThat(response.exportUrl()).isEqualTo("/api/agent/suggestions/%s/export".formatted(suggestionId));
    }

    @Test
    void exportsDocumentDraftSuggestionAsMarkdown() {
        UUID userId = UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        AgentSuggestion draft = AgentSuggestion.draft(
                userId,
                null,
                UUID.randomUUID(),
                null,
                AgentSuggestionType.DOCUMENT_DRAFT,
                Map.of(
                        "title", "Proposal draft",
                        "description", "# Proposal\n\nContent"
                ),
                null
        );
        ReflectionTestUtils.setField(draft, "id", suggestionId);
        AgentSuggestionRepository repository = mock(AgentSuggestionRepository.class);
        when(repository.findById(suggestionId)).thenReturn(Optional.of(draft));

        var result = new AgentSuggestionQueryService(repository, mock(ProjectMembershipPublicService.class))
                .exportDocumentDraft(userId, suggestionId);

        assertThat(result.fileName()).isEqualTo("Proposal draft.md");
        assertThat(new String(result.content())).isEqualTo("# Proposal\n\nContent");
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
