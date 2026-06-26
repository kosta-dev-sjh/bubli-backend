package com.bubli.resource.service;

import com.bubli.resource.entity.ResourceFile;
import com.bubli.resource.entity.ResourceStorageDeleteRetryRecord;
import com.bubli.resource.repository.ResourceStorageDeleteRetryRecordRepository;
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
		ResourceStorageDeleteRetryRecordRepository repository = mock(ResourceStorageDeleteRetryRecordRepository.class);
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

		ArgumentCaptor<ResourceStorageDeleteRetryRecord> captor =
				ArgumentCaptor.forClass(ResourceStorageDeleteRetryRecord.class);
		verify(repository).save(captor.capture());
		ResourceStorageDeleteRetryRecord record = captor.getValue();
		assertThat(record.getResourceId()).isEqualTo(resourceId);
		assertThat(record.getFileId()).isEqualTo(fileId);
		assertThat(record.getStorageKey()).isEqualTo("resources/%s/file.pdf".formatted(resourceId));
		assertThat(record.getStatus()).isEqualTo(ResourceStorageDeleteStatus.PENDING);
		assertThat(record.getRetryCount()).isZero();
		assertThat(record.getLastErrorMessage()).isEqualTo("storage unavailable");
	}

	@Test
	void recordFailedDeleteTruncatesLongErrorMessage() {
		ResourceStorageDeleteRetryRecordRepository repository = mock(ResourceStorageDeleteRetryRecordRepository.class);
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

		ArgumentCaptor<ResourceStorageDeleteRetryRecord> captor =
				ArgumentCaptor.forClass(ResourceStorageDeleteRetryRecord.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getLastErrorMessage()).hasSize(1_000);
	}
}
