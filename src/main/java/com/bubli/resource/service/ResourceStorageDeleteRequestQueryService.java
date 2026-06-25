package com.bubli.resource.service;

import com.bubli.global.response.PageResponse;
import com.bubli.resource.dto.ResourceStorageDeleteRequestResult;
import com.bubli.resource.repository.ResourceStorageDeleteRequestRepository;
import com.bubli.resource.type.ResourceStorageDeleteStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourceStorageDeleteRequestQueryService {

	private final ResourceStorageDeleteRequestRepository resourceStorageDeleteRequestRepository;

	@Transactional(readOnly = true)
	public PageResponse<ResourceStorageDeleteRequestResult> getDeadLetterRequests(Pageable pageable) {
		Page<ResourceStorageDeleteRequestResult> page = resourceStorageDeleteRequestRepository
				.findByStatus(ResourceStorageDeleteStatus.DEAD_LETTER, withDefaultSort(pageable))
				.map(ResourceStorageDeleteRequestResult::from);
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
				Sort.by("updatedAt").ascending()
		);
	}
}
