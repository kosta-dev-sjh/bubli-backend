package com.bubli.agent.service;

import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.entity.GeneratedDocument;
import com.bubli.agent.repository.GeneratedDocumentRepository;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.project.service.ProjectMembershipPublicService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeneratedDocumentServiceTest {

    @Test
    void createsGeneratedDocumentFromDocumentDraftSuggestion() {
        GeneratedDocumentRepository repository = mock(GeneratedDocumentRepository.class);
        when(repository.findBySuggestionId(any())).thenReturn(Optional.empty());
        when(repository.save(any(GeneratedDocument.class))).thenAnswer(invocation -> {
            GeneratedDocument document = invocation.getArgument(0);
            ReflectionTestUtils.setField(document, "id", UUID.randomUUID());
            return document;
        });
        UUID reviewerId = UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        AgentSuggestion suggestion = suggestion(suggestionId, roomId, Map.of(
                "title", "회의록 초안",
                "documentType", "MEETING_NOTE",
                "contentMarkdown", "# Meeting Note",
                "instruction", "decisions only",
                "sourceResourceIds", List.of(UUID.randomUUID().toString())
        ));

        GeneratedDocument document = new GeneratedDocumentService(
                repository,
                mock(ProjectMembershipPublicService.class)
        ).createFromSuggestion(reviewerId, suggestion);

        assertThat(document.getSuggestionId()).isEqualTo(suggestionId);
        assertThat(document.getRoomId()).isEqualTo(roomId);
        assertThat(document.getTitle()).isEqualTo("회의록 초안");
        assertThat(document.getDocumentType()).isEqualTo("MEETING_NOTE");
        assertThat(document.getContentMarkdown()).isEqualTo("# Meeting Note");
        assertThat(document.getMetadataJson()).containsEntry("reviewerId", reviewerId.toString());
        verify(repository).save(any(GeneratedDocument.class));
    }

    @Test
    void returnsExistingDocumentForSameSuggestion() {
        GeneratedDocumentRepository repository = mock(GeneratedDocumentRepository.class);
        UUID suggestionId = UUID.randomUUID();
        GeneratedDocument existing = GeneratedDocument.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                suggestionId,
                null,
                "existing",
                "GENERAL",
                "content",
                Map.of()
        );
        when(repository.findBySuggestionId(suggestionId)).thenReturn(Optional.of(existing));

        GeneratedDocument result = new GeneratedDocumentService(
                repository,
                mock(ProjectMembershipPublicService.class)
        ).createFromSuggestion(UUID.randomUUID(), suggestion(suggestionId, UUID.randomUUID(), Map.of(
                "title", "new",
                "contentMarkdown", "new content"
        )));

        assertThat(result).isSameAs(existing);
        verify(repository, never()).save(any());
    }

    @Test
    void listsRoomDocumentsAfterMembershipCheck() {
        GeneratedDocumentRepository repository = mock(GeneratedDocumentRepository.class);
        ProjectMembershipPublicService membershipService = mock(ProjectMembershipPublicService.class);
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        when(repository.findByRoomId(roomId, pageable)).thenReturn(new PageImpl<>(List.of(
                GeneratedDocument.create(
                        userId,
                        roomId,
                        UUID.randomUUID(),
                        null,
                        "doc",
                        "GENERAL",
                        "content",
                        Map.of()
                )
        ), pageable, 1));

        var result = new GeneratedDocumentService(repository, membershipService)
                .getRoomDocuments(userId, roomId, pageable);

        assertThat(result.getItems()).hasSize(1);
        verify(membershipService).assertActiveMember(userId, roomId);
    }

    private AgentSuggestion suggestion(UUID suggestionId, UUID roomId, Map<String, Object> payload) {
        AgentSuggestion suggestion = AgentSuggestion.draft(
                UUID.randomUUID(),
                roomId,
                UUID.randomUUID(),
                null,
                AgentSuggestionType.DOCUMENT_DRAFT,
                payload,
                null
        );
        ReflectionTestUtils.setField(suggestion, "id", suggestionId);
        return suggestion;
    }
}
