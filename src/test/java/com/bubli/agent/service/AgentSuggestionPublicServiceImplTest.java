package com.bubli.agent.service;

import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.repository.AgentSuggestionRepository;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AgentSuggestionPublicServiceImplTest {

	@Mock
	AgentSuggestionRepository agentSuggestionRepository;

	@InjectMocks
	AgentSuggestionPublicServiceImpl agentSuggestionPublicService;

	@Test
	void getReviewRequiredSummariesReturnsDraftAndHeldSuggestionTitles() {
		UUID userId = UUID.randomUUID();
		AgentSuggestion suggestion = AgentSuggestion.draft(
				userId,
				null,
				UUID.randomUUID(),
				null,
				AgentSuggestionType.QUESTION,
				Map.of("title", "검수 기준 확인"),
				Map.of()
		);
		given(agentSuggestionRepository.findByUserIdAndStatusIn(eq(userId), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
				.willReturn(new PageImpl<>(List.of(suggestion)));

		List<String> summaries = agentSuggestionPublicService.getReviewRequiredSummaries(userId, 5);

		assertThat(summaries).containsExactly("확인 질문: 검수 기준 확인");
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(agentSuggestionRepository).findByUserIdAndStatusIn(
				eq(userId),
				eq(List.of(AgentSuggestionStatus.DRAFT, AgentSuggestionStatus.HELD)),
				pageableCaptor.capture()
		);
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
	}
}
