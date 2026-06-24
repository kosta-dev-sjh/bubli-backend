package com.bubli.resource.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.project.service.RoomAccessService;
import com.bubli.resource.dto.CreateResourceCommand;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.entity.Resource;
import com.bubli.resource.repository.ResourceRepository;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.resource.type.ResourceVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceService {

	private static final String PERSONAL_SCOPE = "personal";

	private final ResourceRepository resourceRepository;
	private final RoomAccessService roomAccessService;

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
		Resource resource = resourceRepository.findByIdAndDeletedAtIsNull(resourceId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_404_001));
		validateReadable(userId, resource);
		return ResourceResult.from(resource);
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

	private void validateCreateCommand(UUID userId, CreateResourceCommand command) {
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

	private void validateRoomResourceAccess(UUID userId, UUID roomId) {
		if (!roomAccessService.isActiveMember(userId, roomId)) {
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
}
