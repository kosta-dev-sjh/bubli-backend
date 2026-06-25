package com.bubli.resource.service;

import com.bubli.global.response.PageResponse;
import com.bubli.resource.dto.ResourceStorageDeleteRequestResult;
import com.bubli.resource.entity.ResourceStorageDeleteRequest;
import com.bubli.resource.repository.ResourceStorageDeleteRequestRepository;
import com.bubli.resource.type.ResourceStorageDeleteStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceStorageDeleteRequestQueryServiceTest {

	@Test
	void getDeadLetterRequestsReturnsDeadLetterRequestsWithDefaultSort() {
		ResourceStorageDeleteRequestRepository repository = mock(ResourceStorageDeleteRequestRepository.class);
		ResourceStorageDeleteRequestQueryService service = new ResourceStorageDeleteRequestQueryService(repository);
		ResourceStorageDeleteRequest request = deadLetterRequest();
		PageRequest pageable = PageRequest.of(0, 20);
		when(repository.findByStatus(
				ResourceStorageDeleteStatus.DEAD_LETTER,
				PageRequest.of(0, 20, Sort.by("updatedAt").ascending())
		)).thenReturn(new PageImpl<>(List.of(request), pageable, 1));

		PageResponse<ResourceStorageDeleteRequestResult> result = service.getDeadLetterRequests(pageable);

		assertThat(result.getItems()).hasSize(1);
		ResourceStorageDeleteRequestResult item = result.getItems().getFirst();
		assertThat(item.id()).isEqualTo(request.getId());
		assertThat(item.resourceId()).isEqualTo(request.getResourceId());
		assertThat(item.fileId()).isEqualTo(request.getFileId());
		assertThat(item.storageKey()).isEqualTo("storage-key");
		assertThat(item.status()).isEqualTo(ResourceStorageDeleteStatus.DEAD_LETTER);
		assertThat(item.retryCount()).isEqualTo(3);
		assertThat(item.lastErrorMessage()).isEqualTo(ResourceStorageDeleteRetryWorker.DEAD_LETTER_MESSAGE);
		assertThat(result.getTotalElements()).isEqualTo(1);
	}

	@Test
	void getDeadLetterRequestsKeepsExplicitSort() {
		ResourceStorageDeleteRequestRepository repository = mock(ResourceStorageDeleteRequestRepository.class);
		ResourceStorageDeleteRequestQueryService service = new ResourceStorageDeleteRequestQueryService(repository);
		PageRequest pageable = PageRequest.of(1, 10, Sort.by("createdAt").descending());
		when(repository.findByStatus(ResourceStorageDeleteStatus.DEAD_LETTER, pageable))
				.thenReturn(new PageImpl<>(List.of(), pageable, 0));

		service.getDeadLetterRequests(pageable);

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(repository).findByStatus(
				org.mockito.ArgumentMatchers.eq(ResourceStorageDeleteStatus.DEAD_LETTER),
				pageableCaptor.capture()
		);
		assertThat(pageableCaptor.getValue()).isEqualTo(pageable);
	}

	private ResourceStorageDeleteRequest deadLetterRequest() {
		ResourceStorageDeleteRequest request = ResourceStorageDeleteRequest.create(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"storage-key",
				"first failure"
		);
		UUID requestId = UUID.randomUUID();
		Instant createdAt = Instant.parse("2026-06-25T00:00:00Z");
		Instant updatedAt = Instant.parse("2026-06-25T00:10:00Z");
		ReflectionTestUtils.setField(request, "id", requestId);
		ReflectionTestUtils.setField(request, "retryCount", 3);
		ReflectionTestUtils.setField(request, "createdAt", createdAt);
		ReflectionTestUtils.setField(request, "updatedAt", updatedAt);
		request.markDeadLetter(ResourceStorageDeleteRetryWorker.DEAD_LETTER_MESSAGE);
		return request;
	}
}
