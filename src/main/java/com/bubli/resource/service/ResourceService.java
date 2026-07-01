package com.bubli.resource.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.resource.dto.CreateResourceCommand;
import com.bubli.resource.dto.CreateResourceVersionCommand;
import com.bubli.resource.dto.ResourceCommentResult;
import com.bubli.resource.dto.ResourceDownloadUrlResult;
import com.bubli.resource.dto.ResourceRelatedResult;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.dto.ResourceSummaryResult;
import com.bubli.resource.dto.ResourceVersionResult;
import com.bubli.resource.dto.UploadResourceCommand;
import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceComment;
import com.bubli.resource.entity.ResourceFile;
import com.bubli.resource.entity.ResourceRelation;
import com.bubli.resource.entity.ResourceSummary;
import com.bubli.resource.entity.ResourceVersion;
import com.bubli.resource.repository.ResourceCommentRepository;
import com.bubli.resource.repository.ResourceFileRepository;
import com.bubli.resource.repository.ResourceRelationRepository;
import com.bubli.resource.repository.ResourceRepository;
import com.bubli.resource.repository.ResourceSummaryRepository;
import com.bubli.resource.repository.ResourceVersionRepository;
import com.bubli.resource.storage.StorageDownloadUrl;
import com.bubli.resource.storage.StorageDownloadUrlProvider;
import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.resource.type.ResourceVisibility;
import com.bubli.storage.dto.FileUploadResult;
import com.bubli.storage.service.StoragePublicService;
import com.bubli.storage.service.StorageUsagePublicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResourceService {

	private static final String PERSONAL_SCOPE = "personal";
	private static final long DEFAULT_MAX_UPLOAD_SIZE_BYTES = 104_857_600L;

	private final ResourceRepository resourceRepository;
	private final ResourceCommentRepository resourceCommentRepository;
	private final ResourceFileRepository resourceFileRepository;
	private final ResourceRelationRepository resourceRelationRepository;
	private final ResourceSummaryRepository resourceSummaryRepository;
	private final ResourceVersionRepository resourceVersionRepository;
	private final StorageDownloadUrlProvider storageDownloadUrlProvider;
	private final ResourceStorageDeleteRetryRecorder storageDeleteRetryRecorder;
	private final StoragePublicService storagePublicService;
	private final StorageUsagePublicService storageUsagePublicService;
	private final ProjectMembershipPublicService projectMembershipPublicService;

	@Value("${storage.max-upload-size-bytes:104857600}")
	private long maxUploadSizeBytes = DEFAULT_MAX_UPLOAD_SIZE_BYTES;

	@Value("${storage.allowed-mime-types:}")
	private String allowedMimeTypes = "";

	@Transactional(readOnly = true)
	public PageResponse<ResourceResult> getPersonalResources(UUID userId, String scope, Pageable pageable) {
		if (scope != null && !PERSONAL_SCOPE.equalsIgnoreCase(scope)) {
			throw new BusinessException(ErrorCode.RESOURCE_400_001);
		}
		Page<ResourceResult> page = resourceRepository
				.findByOwnerIdAndVisibilityAndDeletedAtIsNull(
						userId,
						ResourceVisibility.PERSONAL,
						withDefaultSort(pageable)
				)
				.map(ResourceResult::from);
		return toPageResponse(page);
	}

	@Transactional(readOnly = true)
	public PageResponse<ResourceResult> getRoomResources(UUID userId, UUID roomId, Pageable pageable) {
		validateRoomResourceAccess(userId, roomId);
		Page<ResourceResult> page = resourceRepository
				.findByRoomIdAndVisibilityAndDeletedAtIsNull(
						roomId,
						ResourceVisibility.ROOM_SHARED,
						withDefaultSort(pageable)
				)
				.map(ResourceResult::from);
		return toPageResponse(page);
	}

	@Transactional(readOnly = true)
	public ResourceResult getResource(UUID userId, UUID resourceId) {
		return ResourceResult.from(getReadableResource(userId, resourceId));
	}

	@Transactional(readOnly = true)
	public PageResponse<ResourceCommentResult> getResourceComments(UUID userId, UUID resourceId, Pageable pageable) {
		getReadableResource(userId, resourceId);
		Page<ResourceCommentResult> page = resourceCommentRepository
				.findByResourceIdAndDeletedAtIsNull(resourceId, withCommentDefaultSort(pageable))
				.map(ResourceCommentResult::from);
		return toCommentPageResponse(page);
	}

	@Transactional(readOnly = true)
	public PageResponse<ResourceVersionResult> getResourceVersions(UUID userId, UUID resourceId, Pageable pageable) {
		getReadableResource(userId, resourceId);
		Page<ResourceVersionResult> page = resourceVersionRepository
				.findByResourceId(resourceId, withVersionDefaultSort(pageable))
				.map(this::toVersionResult);
		return toVersionPageResponse(page);
	}

	@Transactional(readOnly = true)
	public ResourceSummaryResult getResourceSummary(UUID userId, UUID resourceId) {
		getReadableResource(userId, resourceId);
		ResourceSummary summary = resourceSummaryRepository.findFirstByResourceIdOrderByUpdatedAtDescIdDesc(resourceId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_404_004));
		return ResourceSummaryResult.from(summary);
	}

	@Transactional(readOnly = true)
	public java.util.List<ResourceSummaryResult> getRecentRoomSummaries(UUID userId, UUID roomId, int limit) {
		validateRoomResourceAccess(userId, roomId);
		int size = Math.max(1, Math.min(limit, 10));
		return resourceSummaryRepository.findRecentRoomSummaries(
						roomId,
						ResourceVisibility.ROOM_SHARED,
						PageRequest.of(0, size)
				).stream()
				.map(ResourceSummaryResult::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<ResourceResult> findLatestRoomResource(UUID userId, UUID roomId, List<String> titleKeywords) {
		validateRoomResourceAccess(userId, roomId);
		List<String> keywords = normalizeTitleKeywords(titleKeywords);
		if (keywords.isEmpty()) {
			return Optional.empty();
		}
		return resourceRepository.findLatestRoomResourceCandidates(
						roomId,
						ResourceVisibility.ROOM_SHARED,
						List.of(ResourceStatus.READY, ResourceStatus.ANALYZED),
						keywords.get(0),
						keywords.get(1),
						keywords.get(2),
						PageRequest.of(0, 1)
				).stream()
				.findFirst()
				.map(ResourceResult::from);
	}

	@Transactional(readOnly = true)
	public Optional<ResourceResult> findLatestRoomFile(UUID userId, UUID roomId) {
		validateRoomResourceAccess(userId, roomId);
		return resourceRepository.findByRoomIdAndVisibilityAndKindAndDeletedAtIsNullAndStatusIn(
						roomId,
						ResourceVisibility.ROOM_SHARED,
						ResourceKind.FILE,
						List.of(ResourceStatus.READY, ResourceStatus.ANALYZED),
						PageRequest.of(0, 1, Sort.by("createdAt").descending().and(Sort.by("id").descending()))
				).stream()
				.findFirst()
				.map(ResourceResult::from);
	}

	private List<String> normalizeTitleKeywords(List<String> titleKeywords) {
		List<String> keywords = titleKeywords == null ? List.of() : titleKeywords.stream()
				.filter(StringUtils::hasText)
				.map(String::trim)
				.distinct()
				.limit(3)
				.toList();
		if (keywords.isEmpty()) {
			return List.of();
		}
		if (keywords.size() == 1) {
			return List.of(keywords.get(0), keywords.get(0), keywords.get(0));
		}
		if (keywords.size() == 2) {
			return List.of(keywords.get(0), keywords.get(1), keywords.get(1));
		}
		return keywords;
	}

	@Transactional(readOnly = true)
	public PageResponse<ResourceRelatedResult> getRelatedResources(UUID userId, UUID resourceId, Pageable pageable) {
		getReadableResource(userId, resourceId);
		Page<ResourceRelatedResult> page = resourceRelationRepository
				.findByResourceId(resourceId, withRelationDefaultSort(pageable))
				.map(relation -> toRelatedResult(userId, relation));
		return toRelatedPageResponse(page);
	}

	@Transactional(readOnly = true)
	public ResourceDownloadUrlResult getResourceDownloadUrl(UUID userId, UUID resourceId) {
		getReadableResource(userId, resourceId);
		ResourceVersion version = resourceVersionRepository.findFirstByResourceIdOrderByVersionNoDescIdDesc(resourceId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_404_003));
		ResourceFile file = resourceFileRepository.findById(version.getFileId())
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_404_003));
		StorageDownloadUrl downloadUrl = storageDownloadUrlProvider.issueDownloadUrl(
				file.getStorageKey(),
				file.getOriginalName()
		);
		return new ResourceDownloadUrlResult(
				resourceId,
				file.getId(),
				version.getVersionNo(),
				downloadUrl.url(),
				downloadUrl.expiresAt(),
				file.getOriginalName(),
				file.getMimeType(),
				file.getSizeBytes()
		);
	}

	@Transactional
	public ResourceResult create(UUID userId, CreateResourceCommand command) {
		validateCreateCommand(userId, command);
		UUID roomId = command.visibility() == ResourceVisibility.ROOM_SHARED ? command.roomId() : null;
		Resource resource = Resource.create(
				userId,
				roomId,
				command.title(),
				command.kind(),
				command.visibility(),
				ResourceStatus.READY
		);
		return ResourceResult.from(resourceRepository.save(resource));
	}

	@Transactional
	public ResourceResult upload(UUID userId, UploadResourceCommand command) {
		validateUploadCommand(userId, command);
		UUID roomId = command.visibility() == ResourceVisibility.ROOM_SHARED ? command.roomId() : null;
		Resource resource = resourceRepository.save(Resource.create(
				userId,
				roomId,
				command.title(),
				command.kind(),
				command.visibility(),
				ResourceStatus.READY
		));
		FileUploadResult uploaded = null;
		boolean storageUsageRecorded = false;
		try {
			recordStorageUsage(userId, roomId, command);
			storageUsageRecorded = true;
			uploaded = storagePublicService.save(
					storageKey(resource.getId(), command.originalName()),
					command.originalName(),
					command.mimeType(),
					command.content()
			);
			ResourceFile file = resourceFileRepository.save(ResourceFile.create(
					resource.getId(),
					uploaded.storageKey(),
					uploaded.originalName(),
					uploaded.mimeType(),
					uploaded.sizeBytes(),
					uploaded.checksum()
			));
			int nextVersionNo = resourceVersionRepository.findMaxVersionNo(resource.getId()) + 1;
			resourceVersionRepository.save(ResourceVersion.create(
					resource.getId(),
					nextVersionNo,
					file.getId(),
					userId
			));
		} catch (RuntimeException e) {
			deleteUploadedObject(uploaded, e);
			releaseRecordedStorageUsage(storageUsageRecorded, userId, roomId, command, e);
			throw e;
		}
		return ResourceResult.from(resource);
	}

	@Transactional
	public ResourceResult updateResource(UUID userId, UUID resourceId, String title) {
		Resource resource = getReadableResource(userId, resourceId);
		if (title != null && title.isBlank()) {
			throw new BusinessException(ErrorCode.RESOURCE_400_001);
		}
		resource.updateTitle(title);
		return ResourceResult.from(resource);
	}

	@Transactional
	public void deleteResource(UUID userId, UUID resourceId) {
		Resource resource = getReadableResource(userId, resourceId);
		var files = resourceFileRepository.findByResourceId(resourceId);
		long usedBytes = files.stream()
				.mapToLong(ResourceFile::getSizeBytes)
				.sum();
		resource.markDeleted(Instant.now());
		deleteStoredFiles(files);
		releaseStorageUsage(resource.getOwnerId(), resource.getRoomId(), resource.getVisibility(), usedBytes);
	}

	@Transactional
	public ResourceCommentResult createComment(UUID userId, UUID resourceId, UUID parentId, String body) {
		getReadableResource(userId, resourceId);
		validateCommentBody(body);
		validateParentComment(resourceId, parentId);
		ResourceComment comment = ResourceComment.create(resourceId, userId, parentId, body);
		return ResourceCommentResult.from(resourceCommentRepository.save(comment));
	}

	@Transactional
	public ResourceVersionResult createVersion(UUID userId, UUID resourceId, CreateResourceVersionCommand command) {
		getReadableResource(userId, resourceId);
		ResourceFile file = resourceFileRepository.save(ResourceFile.create(
				resourceId,
				command.storageKey(),
				command.originalName(),
				command.mimeType(),
				command.sizeBytes(),
				command.checksum()
		));
		int nextVersionNo = resourceVersionRepository.findMaxVersionNo(resourceId) + 1;
		ResourceVersion version = resourceVersionRepository.save(ResourceVersion.create(
				resourceId,
				nextVersionNo,
				file.getId(),
				userId
		));
		return ResourceVersionResult.from(version, file);
	}

	@Transactional
	public ResourceCommentResult updateComment(UUID userId, UUID commentId, String body) {
		ResourceComment comment = getReadableComment(commentId);
		getReadableResource(userId, comment.getResourceId());
		checkCommentAuthor(userId, comment);
		validateCommentBody(body);
		comment.updateBody(body);
		return ResourceCommentResult.from(comment);
	}

	@Transactional
	public void deleteComment(UUID userId, UUID commentId) {
		ResourceComment comment = getReadableComment(commentId);
		getReadableResource(userId, comment.getResourceId());
		checkCommentAuthor(userId, comment);
		comment.markDeleted(Instant.now());
	}

	private void validateCreateCommand(UUID userId, CreateResourceCommand command) {
		if (command == null || !StringUtils.hasText(command.title())
				|| command.kind() == null || command.visibility() == null) {
			throw new BusinessException(ErrorCode.RESOURCE_400_001);
		}
		if (command.visibility() == ResourceVisibility.PERSONAL) {
			if (command.roomId() != null) {
				throw new BusinessException(ErrorCode.RESOURCE_400_001);
			}
			return;
		}
		if (command.visibility() == ResourceVisibility.ROOM_SHARED) {
			if (command.roomId() == null) {
				throw new BusinessException(ErrorCode.RESOURCE_400_001);
			}
			validateRoomResourceAccess(userId, command.roomId());
			return;
		}
		throw new BusinessException(ErrorCode.RESOURCE_400_001);
	}

	private void validateUploadCommand(UUID userId, UploadResourceCommand command) {
		if (command == null || command.content() == null || command.content().length == 0
				|| !StringUtils.hasText(command.originalName()) || !StringUtils.hasText(command.mimeType())) {
			throw new BusinessException(ErrorCode.RESOURCE_400_001);
		}
		if (command.content().length > maxUploadSizeBytes || !isAllowedMimeType(command.mimeType())) {
			throw new BusinessException(ErrorCode.RESOURCE_400_001);
		}
		validateCreateCommand(userId, command.toCreateResourceCommand());
	}

	private boolean isAllowedMimeType(String mimeType) {
		if (!StringUtils.hasText(allowedMimeTypes)) {
			return true;
		}
		for (String allowedMimeType : allowedMimeTypes.split(",")) {
			if (mimeType.equalsIgnoreCase(allowedMimeType.trim())) {
				return true;
			}
		}
		return false;
	}

	private String storageKey(UUID resourceId, String originalName) {
		String extension = StringUtils.getFilenameExtension(originalName);
		String suffix = StringUtils.hasText(extension) ? "." + extension : "";
		return "resources/%s/%s%s".formatted(resourceId, UUID.randomUUID(), suffix);
	}

	private void deleteUploadedObject(FileUploadResult uploaded, RuntimeException cause) {
		if (uploaded == null || !StringUtils.hasText(uploaded.storageKey())) {
			return;
		}
		try {
			storagePublicService.delete(uploaded.storageKey());
		} catch (RuntimeException deleteException) {
			cause.addSuppressed(deleteException);
		}
	}

	private void deleteStoredFiles(Iterable<ResourceFile> files) {
		for (ResourceFile file : files) {
			try {
				storagePublicService.delete(file.getStorageKey());
			} catch (RuntimeException exception) {
				log.warn("Failed to delete resource file from storage. resourceId={}, fileId={}, storageKey={}",
						file.getResourceId(),
						file.getId(),
						file.getStorageKey(),
						exception);
				recordStorageDeleteRetry(file, exception);
			}
		}
	}

	private void recordStorageDeleteRetry(ResourceFile file, RuntimeException cause) {
		try {
			storageDeleteRetryRecorder.recordFailedDelete(file, cause);
		} catch (RuntimeException recorderException) {
			log.warn("Failed to record resource storage delete retry. resourceId={}, fileId={}, storageKey={}",
					file.getResourceId(),
					file.getId(),
					file.getStorageKey(),
					recorderException);
		}
	}

	private void recordStorageUsage(UUID userId, UUID roomId, UploadResourceCommand command) {
		long sizeBytes = command.content().length;
		if (command.visibility() == ResourceVisibility.PERSONAL) {
			storageUsagePublicService.recordPersonalUpload(userId, sizeBytes);
			return;
		}
		storageUsagePublicService.recordRoomUpload(roomId, sizeBytes);
	}

	private void releaseRecordedStorageUsage(
			boolean storageUsageRecorded,
			UUID userId,
			UUID roomId,
			UploadResourceCommand command,
			RuntimeException cause
	) {
		if (!storageUsageRecorded) {
			return;
		}
		long sizeBytes = command.content().length;
		try {
			if (command.visibility() == ResourceVisibility.PERSONAL) {
				storageUsagePublicService.releasePersonalUsage(userId, sizeBytes);
				return;
			}
			storageUsagePublicService.releaseRoomUsage(roomId, sizeBytes);
		} catch (RuntimeException releaseException) {
			cause.addSuppressed(releaseException);
		}
	}

	private void releaseStorageUsage(UUID userId, UUID roomId, ResourceVisibility visibility, long sizeBytes) {
		if (sizeBytes <= 0) {
			return;
		}
		if (visibility == ResourceVisibility.PERSONAL) {
			storageUsagePublicService.releasePersonalUsage(userId, sizeBytes);
			return;
		}
		storageUsagePublicService.releaseRoomUsage(roomId, sizeBytes);
	}

	private void validateReadable(UUID userId, Resource resource) {
		if (resource.getVisibility() == ResourceVisibility.PERSONAL) {
			if (!resource.getOwnerId().equals(userId)) {
				throw new BusinessException(ErrorCode.RESOURCE_403_001);
			}
			return;
		}
		if (resource.getVisibility() == ResourceVisibility.ROOM_SHARED && resource.getRoomId() != null) {
			validateRoomResourceAccess(userId, resource.getRoomId());
			return;
		}
		throw new BusinessException(ErrorCode.RESOURCE_403_001);
	}

	private Resource getReadableResource(UUID userId, UUID resourceId) {
		Resource resource = resourceRepository.findByIdAndDeletedAtIsNull(resourceId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_404_001));
		validateReadable(userId, resource);
		return resource;
	}

	private ResourceComment getReadableComment(UUID commentId) {
		return resourceCommentRepository.findByIdAndDeletedAtIsNull(commentId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_404_002));
	}

	private ResourceVersionResult toVersionResult(ResourceVersion version) {
		ResourceFile file = resourceFileRepository.findById(version.getFileId())
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_404_003));
		return ResourceVersionResult.from(version, file);
	}

	private ResourceRelatedResult toRelatedResult(UUID userId, ResourceRelation relation) {
		Resource relatedResource = getReadableResource(userId, relation.getRelatedResourceId());
		return ResourceRelatedResult.from(relation, relatedResource);
	}

	private void validateParentComment(UUID resourceId, UUID parentId) {
		if (parentId == null) {
			return;
		}
		ResourceComment parent = getReadableComment(parentId);
		if (!resourceId.equals(parent.getResourceId())) {
			throw new BusinessException(ErrorCode.RESOURCE_400_001);
		}
	}

	private void validateCommentBody(String body) {
		if (body == null || body.isBlank()) {
			throw new BusinessException(ErrorCode.RESOURCE_400_001);
		}
	}

	private void checkCommentAuthor(UUID userId, ResourceComment comment) {
		if (!userId.equals(comment.getAuthorId())) {
			throw new BusinessException(ErrorCode.RESOURCE_403_001);
		}
	}

	private void validateRoomResourceAccess(UUID userId, UUID roomId) {
		if (!projectMembershipPublicService.isActiveMember(userId, roomId)) {
			throw new BusinessException(ErrorCode.RESOURCE_403_001);
		}
	}

	private PageResponse<ResourceResult> toPageResponse(Page<ResourceResult> page) {
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext()
		);
	}

	private PageResponse<ResourceCommentResult> toCommentPageResponse(Page<ResourceCommentResult> page) {
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext()
		);
	}

	private PageResponse<ResourceVersionResult> toVersionPageResponse(Page<ResourceVersionResult> page) {
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext()
		);
	}

	private PageResponse<ResourceRelatedResult> toRelatedPageResponse(Page<ResourceRelatedResult> page) {
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext()
		);
	}

	private Pageable withDefaultSort(Pageable pageable) {
		if (pageable.getSort().isSorted()) {
			return pageable;
		}
		return PageRequest.of(
				pageable.getPageNumber(),
				pageable.getPageSize(),
				Sort.by("createdAt").descending().and(Sort.by("id").descending())
		);
	}

	private Pageable withCommentDefaultSort(Pageable pageable) {
		if (pageable.getSort().isSorted()) {
			return pageable;
		}
		return PageRequest.of(
				pageable.getPageNumber(),
				pageable.getPageSize(),
				Sort.by("createdAt").ascending().and(Sort.by("id").ascending())
		);
	}

	private Pageable withVersionDefaultSort(Pageable pageable) {
		if (pageable.getSort().isSorted()) {
			return pageable;
		}
		return PageRequest.of(
				pageable.getPageNumber(),
				pageable.getPageSize(),
				Sort.by("versionNo").descending().and(Sort.by("id").descending())
		);
	}

	private Pageable withRelationDefaultSort(Pageable pageable) {
		if (pageable.getSort().isSorted()) {
			return pageable;
		}
		return PageRequest.of(
				pageable.getPageNumber(),
				pageable.getPageSize(),
				Sort.by("score").descending()
						.and(Sort.by("createdAt").descending())
						.and(Sort.by("id").descending())
		);
	}
}
