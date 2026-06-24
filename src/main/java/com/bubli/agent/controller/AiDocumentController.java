package com.bubli.agent.controller;

import com.bubli.agent.dto.AiDocumentResponse;
import com.bubli.agent.service.AiDocumentService;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AiDocumentController {

	private final ResourceService resourceService;
	private final AiDocumentService aiDocumentService;

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
}
