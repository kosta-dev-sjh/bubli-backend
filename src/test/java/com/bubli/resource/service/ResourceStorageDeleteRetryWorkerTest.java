package com.bubli.resource.service;

import com.bubli.resource.entity.ResourceStorageDeleteRequest;
import com.bubli.resource.repository.ResourceStorageDeleteRequestRepository;
import com.bubli.resource.type.ResourceStorageDeleteStatus;
import com.bubli.storage.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResourceStorageDeleteRetryWorkerTest {

	@Test
	void retryDeleteRequestsDeletesPendingStorageObject() {
		ResourceStorageDeleteRequestRepository repository = mock(ResourceStorageDeleteRequestRepository.class);
		StorageService storageService = mock(StorageService.class);
		ResourceStorageDeleteRetryWorker worker = new ResourceStorageDeleteRetryWorker(repository, storageService);
		ResourceStorageDeleteRequest request = deleteRequest("storage-key");
		when(repository.findByStatusIn(anyCollection(), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(request)));

		int deletedCount = worker.retryDeleteRequests(10, 3);

		assertThat(deletedCount).isEqualTo(1);
		assertThat(request.getStatus()).isEqualTo(ResourceStorageDeleteStatus.DELETED);
		assertThat(request.getLastErrorMessage()).isNull();
		verify(storageService).delete("storage-key");
	}

	@Test
	void retryDeleteRequestsMarksFailedWhenStorageDeleteFails() {
		ResourceStorageDeleteRequestRepository repository = mock(ResourceStorageDeleteRequestRepository.class);
		StorageService storageService = mock(StorageService.class);
		ResourceStorageDeleteRetryWorker worker = new ResourceStorageDeleteRetryWorker(repository, storageService);
		ResourceStorageDeleteRequest request = deleteRequest("storage-key");
		when(repository.findByStatusIn(anyCollection(), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(request)));
		doThrow(new IllegalStateException("storage unavailable"))
				.when(storageService)
				.delete("storage-key");

		int deletedCount = worker.retryDeleteRequests(10, 3);

		assertThat(deletedCount).isZero();
		assertThat(request.getStatus()).isEqualTo(ResourceStorageDeleteStatus.FAILED);
		assertThat(request.getRetryCount()).isEqualTo(1);
		assertThat(request.getLastErrorMessage()).isEqualTo("storage unavailable");
	}

	@Test
	void retryDeleteRequestsMarksDeadLetterWhenRetryCountReachesLimit() {
		ResourceStorageDeleteRequestRepository repository = mock(ResourceStorageDeleteRequestRepository.class);
		StorageService storageService = mock(StorageService.class);
		ResourceStorageDeleteRetryWorker worker = new ResourceStorageDeleteRetryWorker(repository, storageService);
		ResourceStorageDeleteRequest request = deleteRequest("storage-key");
		ReflectionTestUtils.setField(request, "retryCount", 2);
		when(repository.findByStatusIn(anyCollection(), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(request)));
		doThrow(new IllegalStateException("storage unavailable"))
				.when(storageService)
				.delete("storage-key");

		int deletedCount = worker.retryDeleteRequests(10, 3);

		assertThat(deletedCount).isZero();
		assertThat(request.getStatus()).isEqualTo(ResourceStorageDeleteStatus.DEAD_LETTER);
		assertThat(request.getRetryCount()).isEqualTo(3);
		assertThat(request.getLastErrorMessage())
				.isEqualTo(ResourceStorageDeleteRetryWorker.DEAD_LETTER_MESSAGE);
	}

	@Test
	void retryDeleteRequestsDoesNotDeleteWhenAlreadyOverRetryLimit() {
		ResourceStorageDeleteRequestRepository repository = mock(ResourceStorageDeleteRequestRepository.class);
		StorageService storageService = mock(StorageService.class);
		ResourceStorageDeleteRetryWorker worker = new ResourceStorageDeleteRetryWorker(repository, storageService);
		ResourceStorageDeleteRequest request = deleteRequest("storage-key");
		ReflectionTestUtils.setField(request, "retryCount", 3);
		when(repository.findByStatusIn(anyCollection(), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(request)));

		int deletedCount = worker.retryDeleteRequests(10, 3);

		assertThat(deletedCount).isZero();
		assertThat(request.getStatus()).isEqualTo(ResourceStorageDeleteStatus.DEAD_LETTER);
		assertThat(request.getLastErrorMessage())
				.isEqualTo(ResourceStorageDeleteRetryWorker.DEAD_LETTER_MESSAGE);
		verifyNoInteractions(storageService);
	}

	@Test
	void retryDeleteRequestsReturnsZeroWhenBatchSizeIsNotPositive() {
		ResourceStorageDeleteRequestRepository repository = mock(ResourceStorageDeleteRequestRepository.class);
		StorageService storageService = mock(StorageService.class);
		ResourceStorageDeleteRetryWorker worker = new ResourceStorageDeleteRetryWorker(repository, storageService);

		int deletedCount = worker.retryDeleteRequests(0, 3);

		assertThat(deletedCount).isZero();
		verifyNoInteractions(repository, storageService);
	}

	private ResourceStorageDeleteRequest deleteRequest(String storageKey) {
		return ResourceStorageDeleteRequest.create(
				UUID.randomUUID(),
				UUID.randomUUID(),
				storageKey,
				"first failure"
		);
	}
}
