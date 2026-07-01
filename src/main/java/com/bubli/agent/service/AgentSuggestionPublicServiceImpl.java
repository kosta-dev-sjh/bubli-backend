package com.bubli.agent.service;

import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.repository.AgentSuggestionRepository;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentSuggestionPublicServiceImpl implements AgentSuggestionPublicService {

	private static final int DEFAULT_DASHBOARD_SUGGESTION_LIMIT = 5;
	private static final int MAX_DASHBOARD_SUGGESTION_LIMIT = 20;
	private static final List<AgentSuggestionStatus> REVIEW_REQUIRED_STATUSES = List.of(
			AgentSuggestionStatus.DRAFT,
			AgentSuggestionStatus.HELD
	);

	private final AgentSuggestionRepository agentSuggestionRepository;

	@Override
	@Transactional(readOnly = true)
	public List<String> getReviewRequiredSummaries(UUID userId, int limit) {
		return agentSuggestionRepository.findByUserIdAndStatusIn(
						userId,
						REVIEW_REQUIRED_STATUSES,
						PageRequest.of(
								0,
								boundedLimit(limit),
								Sort.by("updatedAt").descending().and(Sort.by("id").descending())
						)
				)
				.stream()
				.map(this::summaryLine)
				.toList();
	}

	private int boundedLimit(int limit) {
		if (limit <= 0) {
			return DEFAULT_DASHBOARD_SUGGESTION_LIMIT;
		}
		return Math.min(limit, MAX_DASHBOARD_SUGGESTION_LIMIT);
	}

	private String summaryLine(AgentSuggestion suggestion) {
		return "%s: %s".formatted(
				label(suggestion.getSuggestionType()),
				title(suggestion.getPayloadJson(), suggestion.getSuggestionType())
		);
	}

	private String label(AgentSuggestionType type) {
		return switch (type) {
			case REQUIREMENT -> "요구사항";
			case TODO, TASK -> "작업";
			case WBS -> "WBS";
			case SCHEDULE -> "일정";
			case QUESTION -> "확인 질문";
			case CONTRACT_FIELD -> "계약 참고값";
			case CONTRACT_REVIEW -> "계약 검토";
			case REVIEW_ITEM -> "검토 항목";
			case DOCUMENT_DRAFT -> "문서 초안";
			case DAILY_SUMMARY -> "하루정리";
			case MEMO -> "메모";
		};
	}

	private String title(Map<String, Object> payload, AgentSuggestionType type) {
		if (payload == null || payload.isEmpty()) {
			return label(type) + " 후보";
		}
		for (String key : List.of("title", "summary", "description", "body", "raw")) {
			Object value = payload.get(key);
			if (value != null && !value.toString().isBlank()) {
				return value.toString();
			}
		}
		return label(type) + " 후보";
	}
}
