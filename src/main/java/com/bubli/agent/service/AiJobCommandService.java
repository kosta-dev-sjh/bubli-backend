package com.bubli.agent.service;

import com.bubli.agent.dto.AgentJobResult;
import com.bubli.agent.dto.CreateAgentJobCommand;
import com.bubli.agent.type.AgentJobType;
import com.bubli.global.locale.SupportedLocale;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.user.service.UserLocalePublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiJobCommandService {

	private final ResourcePublicService resourcePublicService;
	private final ProjectMembershipPublicService projectMembershipPublicService;
	private final AgentJobService agentJobService;
	private final UserLocalePublicService userLocalePublicService;

	@Transactional
	public AgentJobResult createAnalyzeResourceJob(UUID userId, UUID resourceId) {
		ResourceResult resource = resourcePublicService.getReadableResource(userId, resourceId);
		return agentJobService.create(userId, new CreateAgentJobCommand(
				resource.roomId(),
				resource.id(),
				AgentJobType.ANALYZE_RESOURCE,
				localePayload(userId)
		));
	}

	@Transactional
	public AgentJobResult createGenerateRequirementsJob(UUID userId, UUID roomId) {
		projectMembershipPublicService.assertActiveMember(userId, roomId);
		return agentJobService.create(userId, new CreateAgentJobCommand(
				roomId,
				null,
				AgentJobType.GENERATE_REQUIREMENTS,
				localePayload(userId)
		));
	}

	@Transactional
	public AgentJobResult createGenerateTasksJob(UUID userId, UUID roomId) {
		projectMembershipPublicService.assertActiveMember(userId, roomId);
		return agentJobService.create(userId, new CreateAgentJobCommand(
				roomId,
				null,
				AgentJobType.GENERATE_TASKS,
				localePayload(userId)
		));
	}

	@Transactional
	public AgentJobResult createGenerateWbsJob(UUID userId, UUID roomId) {
		projectMembershipPublicService.assertActiveMember(userId, roomId);
		return agentJobService.create(userId, new CreateAgentJobCommand(
				roomId,
				null,
				AgentJobType.GENERATE_WBS,
				localePayload(userId)
		));
	}

	@Transactional
	public AgentJobResult createGenerateQuestionsJob(UUID userId, UUID roomId) {
		projectMembershipPublicService.assertActiveMember(userId, roomId);
		return agentJobService.create(userId, new CreateAgentJobCommand(
				roomId,
				null,
				AgentJobType.GENERATE_QUESTIONS,
				localePayload(userId)
		));
	}

	@Transactional
	public AgentJobResult createReviewContractDocumentsJob(UUID userId, UUID roomId) {
		projectMembershipPublicService.assertActiveMember(userId, roomId);
		return agentJobService.create(userId, new CreateAgentJobCommand(
				roomId,
				null,
				AgentJobType.REVIEW_CONTRACT_DOCUMENTS,
				localePayload(userId)
		));
	}

	@Transactional
	public AgentJobResult createDailySummaryJob(UUID userId) {
		return createDailySummaryJob(userId, null, null);
	}

	@Transactional
	public AgentJobResult createDailySummaryJob(UUID userId, LocalDate summaryDate) {
		return createDailySummaryJob(userId, summaryDate, null);
	}

	@Transactional
	public AgentJobResult createDailySummaryJob(UUID userId, LocalDate summaryDate, String timezone) {
		Map<String, Object> requestPayload = localePayload(userId);
		if (summaryDate != null) {
			requestPayload.put("summaryDate", summaryDate.toString());
		}
		if (timezone != null && !timezone.isBlank()) {
			requestPayload.put("timezone", timezone.trim());
		}
		return agentJobService.create(userId, new CreateAgentJobCommand(
				null,
				null,
				AgentJobType.DAILY_SUMMARY,
				requestPayload
		));
	}

	@Transactional
	public AgentJobResult createDraftDocumentJob(UUID userId, UUID roomId) {
		return createDraftDocumentJob(userId, roomId, null, null, null);
	}

	@Transactional
	public AgentJobResult createDraftDocumentJob(
			UUID userId,
			UUID roomId,
			String documentType,
			List<UUID> sourceResourceIds,
			String instruction
	) {
		projectMembershipPublicService.assertActiveMember(userId, roomId);
		Map<String, Object> requestPayload = localePayload(userId);
		if (documentType != null && !documentType.isBlank()) {
			requestPayload.put("documentType", documentType.trim());
		}
		if (sourceResourceIds != null && !sourceResourceIds.isEmpty()) {
			requestPayload.put("sourceResourceIds", sourceResourceIds.stream().map(UUID::toString).toList());
		}
		if (instruction != null && !instruction.isBlank()) {
			requestPayload.put("instruction", instruction.trim());
		}
		return agentJobService.create(userId, new CreateAgentJobCommand(
				roomId,
				null,
				AgentJobType.DRAFT_DOCUMENT,
				requestPayload
		));
	}

	private Map<String, Object> localePayload(UUID userId) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("locale", resolveLocale(userId));
		return payload;
	}

	private String resolveLocale(UUID userId) {
		return SupportedLocale.normalize(userLocalePublicService.resolveLocaleCode(userId, null));
	}
}
