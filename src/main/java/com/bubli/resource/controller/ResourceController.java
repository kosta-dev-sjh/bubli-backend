package com.bubli.resource.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.response.PageResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.resource.dto.CreateResourceCommentRequest;
import com.bubli.resource.dto.CreateResourceRequest;
import com.bubli.resource.dto.CreateResourceVersionRequest;
import com.bubli.resource.dto.ResourceCommentResponse;
import com.bubli.resource.dto.ResourceCommentResult;
import com.bubli.resource.dto.ResourceDownloadUrlResponse;
import com.bubli.resource.dto.ResourceRelatedResponse;
import com.bubli.resource.dto.ResourceRelatedResult;
import com.bubli.resource.dto.ResourceResponse;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.dto.ResourceSummaryResponse;
import com.bubli.resource.dto.ResourceVersionResponse;
import com.bubli.resource.dto.ResourceVersionResult;
import com.bubli.resource.dto.UpdateResourceCommentRequest;
import com.bubli.resource.dto.UpdateResourceRequest;
import com.bubli.resource.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ResourceController {

	private final ResourceService resourceService;

	@GetMapping("/api/resources")
	public ApiResponse<PageResponse<ResourceResponse>> getPersonalResources(
			@CurrentUser AuthUser authUser,
			@RequestParam(defaultValue = "personal") String scope,
			@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(mapPage(resourceService.getPersonalResources(authUser.userId(), scope, pageable)));
	}

	@GetMapping("/api/project-rooms/{roomId}/resources")
	public ApiResponse<PageResponse<ResourceResponse>> getRoomResources(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(mapPage(resourceService.getRoomResources(authUser.userId(), roomId, pageable)));
	}

	@PostMapping("/api/resources")
	public ApiResponse<ResourceResponse> createResource(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody CreateResourceRequest request
	) {
		return ApiResponse.success(ResourceResponse.from(resourceService.create(authUser.userId(), request.toCommand())));
	}

	@GetMapping("/api/resources/{resourceId}")
	public ApiResponse<ResourceResponse> getResource(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID resourceId
	) {
		return ApiResponse.success(ResourceResponse.from(resourceService.getResource(authUser.userId(), resourceId)));
	}

	@GetMapping("/api/resources/{resourceId}/comments")
	public ApiResponse<PageResponse<ResourceCommentResponse>> getResourceComments(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID resourceId,
			@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(mapCommentPage(
				resourceService.getResourceComments(authUser.userId(), resourceId, pageable)
		));
	}

	@PostMapping("/api/resources/{resourceId}/comments")
	public ApiResponse<ResourceCommentResponse> createResourceComment(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID resourceId,
			@Valid @RequestBody CreateResourceCommentRequest request
	) {
		return ApiResponse.success(ResourceCommentResponse.from(resourceService.createComment(
				authUser.userId(),
				resourceId,
				request.parentId(),
				request.trimmedBody()
		)));
	}

	@GetMapping("/api/resources/{resourceId}/versions")
	public ApiResponse<PageResponse<ResourceVersionResponse>> getResourceVersions(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID resourceId,
			@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(mapVersionPage(
				resourceService.getResourceVersions(authUser.userId(), resourceId, pageable)
		));
	}

	@GetMapping("/api/resources/{resourceId}/summary")
	public ApiResponse<ResourceSummaryResponse> getResourceSummary(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID resourceId
	) {
		return ApiResponse.success(ResourceSummaryResponse.from(
				resourceService.getResourceSummary(authUser.userId(), resourceId)
		));
	}

	@GetMapping("/api/resources/{resourceId}/related")
	public ApiResponse<PageResponse<ResourceRelatedResponse>> getRelatedResources(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID resourceId,
			@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(mapRelatedPage(
				resourceService.getRelatedResources(authUser.userId(), resourceId, pageable)
		));
	}

	@GetMapping("/api/resources/{resourceId}/download-url")
	public ApiResponse<ResourceDownloadUrlResponse> getResourceDownloadUrl(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID resourceId
	) {
		return ApiResponse.success(ResourceDownloadUrlResponse.from(
				resourceService.getResourceDownloadUrl(authUser.userId(), resourceId)
		));
	}

	@PostMapping("/api/resources/{resourceId}/versions")
	public ApiResponse<ResourceVersionResponse> createResourceVersion(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID resourceId,
			@Valid @RequestBody CreateResourceVersionRequest request
	) {
		return ApiResponse.success(ResourceVersionResponse.from(
				resourceService.createVersion(authUser.userId(), resourceId, request.toCommand())
		));
	}

	@PatchMapping("/api/resources/{resourceId}")
	public ApiResponse<ResourceResponse> updateResource(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID resourceId,
			@Valid @RequestBody UpdateResourceRequest request
	) {
		return ApiResponse.success(ResourceResponse.from(
				resourceService.updateResource(authUser.userId(), resourceId, request.trimmedTitle())
		));
	}

	@DeleteMapping("/api/resources/{resourceId}")
	public ApiResponse<Void> deleteResource(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID resourceId
	) {
		resourceService.deleteResource(authUser.userId(), resourceId);
		return ApiResponse.success(null);
	}

	@PatchMapping("/api/resource-comments/{commentId}")
	public ApiResponse<ResourceCommentResponse> updateResourceComment(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID commentId,
			@Valid @RequestBody UpdateResourceCommentRequest request
	) {
		return ApiResponse.success(ResourceCommentResponse.from(
				resourceService.updateComment(authUser.userId(), commentId, request.trimmedBody())
		));
	}

	@DeleteMapping("/api/resource-comments/{commentId}")
	public ApiResponse<Void> deleteResourceComment(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID commentId
	) {
		resourceService.deleteComment(authUser.userId(), commentId);
		return ApiResponse.success(null);
	}

	private PageResponse<ResourceResponse> mapPage(PageResponse<ResourceResult> page) {
		return new PageResponse<>(
				page.getItems().stream()
						.map(ResourceResponse::from)
						.toList(),
				page.getPage(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isHasNext()
		);
	}

	private PageResponse<ResourceCommentResponse> mapCommentPage(PageResponse<ResourceCommentResult> page) {
		return new PageResponse<>(
				page.getItems().stream()
						.map(ResourceCommentResponse::from)
						.toList(),
				page.getPage(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isHasNext()
		);
	}

	private PageResponse<ResourceVersionResponse> mapVersionPage(PageResponse<ResourceVersionResult> page) {
		return new PageResponse<>(
				page.getItems().stream()
						.map(ResourceVersionResponse::from)
						.toList(),
				page.getPage(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isHasNext()
		);
	}

	private PageResponse<ResourceRelatedResponse> mapRelatedPage(PageResponse<ResourceRelatedResult> page) {
		return new PageResponse<>(
				page.getItems().stream()
						.map(ResourceRelatedResponse::from)
						.toList(),
				page.getPage(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isHasNext()
		);
	}
}
