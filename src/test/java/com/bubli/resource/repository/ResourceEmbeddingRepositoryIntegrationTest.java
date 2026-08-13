package com.bubli.resource.repository;

import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.repository.ProjectRoomRepository;
import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceEmbeddingSearchRow;
import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.resource.type.ResourceVisibility;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ResourceEmbeddingRepositoryIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    ResourceEmbeddingRepository embeddingRepository;

    @Autowired
    ResourceRepository resourceRepository;

    @Autowired
    ProjectRoomRepository projectRoomRepository;

    @Autowired
    UserRepository userRepository;

    @Test
    void discoversDistinctDocumentLanguagesForRoomAndResourceScope() {
        SearchContext context = context("document-languages");
        Resource mixed = resource(context, "mixed.pdf");
        Resource korean = resource(context, "korean.pdf");
        insert(mixed, context, 0, "english", vector(1.0F, 0.0F), "en");
        insert(mixed, context, 1, "한국어", vector(0.0F, 1.0F), "ko");
        insert(korean, context, 0, "한국어 전용", vector(0.0F, 1.0F), "ko");

        assertThat(embeddingRepository.findRoomSharedDocumentLanguages(context.room().getId()))
                .containsExactly("en", "ko");
        assertThat(embeddingRepository.findRoomSharedDocumentLanguagesByResourceIds(
                context.room().getId(),
                List.of(korean.getId())
        )).containsExactly("ko");
    }

    @Test
    void semanticLanguagePredicateIsAppliedBeforeLimit() {
        SearchContext context = context("semantic-language");
        Resource english = resource(context, "english.pdf");
        Resource korean = resource(context, "korean.pdf");
        insert(english, context, 0, "closest english evidence", vector(1.0F, 0.0F), "en");
        insert(korean, context, 0, "한국어 근거", vector(0.0F, 1.0F), "ko");

        List<ResourceEmbeddingSearchRow> unrestricted = embeddingRepository.searchRoomShared(
                context.room().getId(),
                vector(1.0F, 0.0F),
                null,
                1
        );
        List<ResourceEmbeddingSearchRow> koreanOnly = embeddingRepository.searchRoomShared(
                context.room().getId(),
                vector(1.0F, 0.0F),
                "ko",
                1
        );

        assertThat(unrestricted).extracting(ResourceEmbeddingSearchRow::getResourceId)
                .containsExactly(english.getId());
        assertThat(koreanOnly).extracting(ResourceEmbeddingSearchRow::getResourceId)
                .containsExactly(korean.getId());
    }

    @Test
    void keywordLanguagePredicateIsAppliedBeforeLimit() {
        SearchContext context = context("keyword-language");
        Resource english = resource(context, "english-keyword.pdf");
        Resource korean = resource(context, "korean-keyword.pdf");
        insert(english, context, 0, "project requirement", vector(1.0F, 0.0F), "en");
        insert(korean, context, 0, "project 한국어 근거", vector(0.0F, 1.0F), "ko");

        List<ResourceEmbeddingSearchRow> koreanOnly = embeddingRepository.searchRoomSharedByKeywords(
                context.room().getId(),
                "project",
                "requirement",
                "",
                "",
                "",
                2,
                "ko",
                1
        );

        assertThat(koreanOnly).extracting(ResourceEmbeddingSearchRow::getResourceId)
                .containsExactly(korean.getId());
    }

    @Test
    void representativeChunkRankingUsesOnlyRequestedLanguage() {
        SearchContext context = context("representative-language");
        Resource resource = resource(context, "mixed-language.pdf");
        insert(resource, context, 0, "english introduction", vector(1.0F, 0.0F), "en");
        insert(resource, context, 1, "한국어 본문", vector(0.0F, 1.0F), "ko");

        List<ResourceEmbeddingSearchRow> koreanOnly = embeddingRepository.findRoomSharedRepresentativeChunks(
                context.room().getId(),
                List.of(resource.getId()),
                "ko",
                1
        );

        assertThat(koreanOnly).singleElement()
                .extracting(ResourceEmbeddingSearchRow::getChunkIndex)
                .isEqualTo(1);
    }

    private SearchContext context(String key) {
        User user = userRepository.saveAndFlush(User.createGoogleUser(
                "google-sub-" + key,
                key,
                "검색 테스트 사용자",
                null,
                "ko",
                "Asia/Seoul"
        ));
        ProjectRoom room = projectRoomRepository.saveAndFlush(ProjectRoom.create(
                user.getId(),
                key,
                null,
                null,
                null,
                null,
                null,
                null
        ));
        return new SearchContext(user, room);
    }

    private Resource resource(SearchContext context, String title) {
        return resourceRepository.saveAndFlush(Resource.create(
                context.user().getId(),
                context.room().getId(),
                title,
                ResourceKind.FILE,
                ResourceVisibility.ROOM_SHARED,
                ResourceStatus.ANALYZED
        ));
    }

    private void insert(
            Resource resource,
            SearchContext context,
            int chunkIndex,
            String chunkText,
            String embedding,
            String documentLanguage
    ) {
        embeddingRepository.insertEmbedding(
                UUID.randomUUID(),
                resource.getId(),
                context.user().getId(),
                context.room().getId(),
                ResourceVisibility.ROOM_SHARED.name(),
                chunkIndex,
                chunkText,
                embedding,
                "{\"documentLanguage\":\"" + documentLanguage + "\"}"
        );
    }

    private String vector(float first, float second) {
        StringBuilder result = new StringBuilder(5_000).append('[').append(first).append(',').append(second);
        for (int index = 2; index < 1_024; index++) {
            result.append(",0.0");
        }
        return result.append(']').toString();
    }

    private record SearchContext(User user, ProjectRoom room) {
    }
}
