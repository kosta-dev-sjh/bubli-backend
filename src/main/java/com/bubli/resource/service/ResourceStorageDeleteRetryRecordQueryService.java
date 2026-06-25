package com.bubli.resource.service;

import com.bubli.global.response.PageResponse;
import com.bubli.resource.dto.ResourceStorageDeleteRetryRecordResult;
import com.bubli.resource.repository.ResourceStorageDeleteRetryRecordRepository;
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
public class ResourceStorageDeleteRetryRecordQueryService {

	private final ResourceStorageDeleteRetryRecordRepository resourceStorageDeleteRetryRecordRepository;

	@Transactional(readOnly = true)
	public PageResponse<ResourceStorageDeleteRetryRecordResult> getDeadLetterRequests(Pageable pageable) {
		Page<ResourceStorageDeleteRetryRecordResult> page = resourceStorageDeleteRetryRecordRepository
				.findByStatus(ResourceStorageDeleteStatus.DEAD_LETTER, withDefaultSort(pageable))
				.map(ResourceStorageDeleteRetryRecordResult::from);
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
