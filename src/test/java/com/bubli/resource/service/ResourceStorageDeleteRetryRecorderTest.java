package com.bubli.resource.service;

import com.bubli.resource.entity.ResourceFile;
import com.bubli.resource.entity.ResourceStorageDeleteRequest;
import com.bubli.resource.repository.ResourceStorageDeleteRequestRepository;
import com.bubli.resource.type.ResourceStorageDeleteStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ResourceStorageDeleteRetryRecorderTest {

	@Test
	void recordFailedDeleteStoresPendingRetryRequest() {
		ResourceStorageDeleteRequestRepository repository = mock(ResourceStorageDeleteRequestRepository.class);
		ResourceStorageDeleteRetryRecorder recorder = new ResourceStorageDeleteRetryRecorder(repository);
		UUID resourceId = UUID.randomUUID();
		UUID fileId = UUID.randomUUID();
		ResourceFile file = ResourceFile.create(
				resourceId,
				"resources/%s/file.pdf".formatted(resourceId),
				"file.pdf",
				"application/pdf",
				3L,
				null
		);
		ReflectionTestUtils.setField(file, "id", fileId);

		recorder.recordFailedDelete(file, new IllegalStateException("storage unavailable"));

		ArgumentCaptor<ResourceStorageDeleteRequest> captor =
				ArgumentCaptor.forClass(ResourceStorageDeleteRequest.class);
		verify(repository).save(captor.capture());
		ResourceStorageDeleteRequest request = captor.getValue();
		assertThat(request.getResourceId()).isEqualTo(resourceId);
		assertThat(request.getFileId()).isEqualTo(fileId);
		assertThat(request.getStorageKey()).isEqualTo("resources/%s/file.pdf".formatted(resourceId));
		assertThat(request.getStatus()).isEqualTo(ResourceStorageDeleteStatus.PENDING);
		assertThat(request.getRetryCount()).isZero();
		assertThat(request.getLastErrorMessage()).isEqualTo("storage unavailable");
	}

	@Test
	void recordFailedDeleteTruncatesLongErrorMessage() {
		ResourceStorageDeleteRequestRepository repository = mock(ResourceStorageDeleteRequestRepository.class);
		ResourceStorageDeleteRetryRecorder recorder = new ResourceStorageDeleteRetryRecorder(repository);
		UUID resourceId = UUID.randomUUID();
		ResourceFile file = ResourceFile.create(
				resourceId,
				"resources/%s/file.pdf".formatted(resourceId),
				"file.pdf",
				"application/pdf",
				3L,
				null
		);

		recorder.recordFailedDelete(file, new IllegalStateException("x".repeat(1_200)));

		ArgumentCaptor<ResourceStorageDeleteRequest> captor =
				ArgumentCaptor.forClass(ResourceStorageDeleteRequest.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getLastErrorMessage()).hasSize(1_000);
	}
}
