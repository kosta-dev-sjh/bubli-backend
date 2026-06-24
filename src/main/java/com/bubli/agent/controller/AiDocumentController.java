package com.bubli.agent.controller;

import com.bubli.agent.dto.AiDocumentResponse;
import com.bubli.agent.dto.AiDocumentResult;
import com.bubli.agent.service.AiDocumentService;
import com.bubli.agent.type.AiDocumentStatus;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.response.PageResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AiDocumentController {

	private final ResourceService resourceService;
	private final AiDocumentService aiDocumentService;

	@GetMapping("/api/project-rooms/{roomId}/ai-documents")
	public ApiResponse<PageResponse<AiDocumentResponse>> getRoomAiDocuments(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@RequestParam(required = false) AiDocumentStatus status,
			@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(mapPage(
				aiDocumentService.getRoomAiDocuments(authUser.userId(), roomId, status, pageable)
		));
	}

	@GetMapping("/api/resources/{resourceId}/ai-document")
	public ApiResponse<AiDocumentResponse> getResourceAiDocument(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID resourceId
	) {
		resourceService.getResource(authUser.userId(), resourceId);
		return ApiResponse.success(AiDocumentResponse.from(
				aiDocumentService.getByResourceId(resourceId)
		));
	}

	private PageResponse<AiDocumentResponse> mapPage(PageResponse<AiDocumentResult> page) {
		return new PageResponse<>(
				page.getItems().stream()
						.map(AiDocumentResponse::from)
						.toList(),
				page.getPage(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isHasNext()
		);
	}
}
