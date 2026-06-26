package com.bubli.storage.service;

import com.bubli.global.error.BusinessException;
import com.bubli.storage.dto.FileUploadResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

	@Mock
	private S3Client s3Client;

	@Test
	void saveUploadsObjectAndReturnsFileMetadata() {
		given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
				.willReturn(PutObjectResponse.builder().build());
		S3StorageService storageService = new S3StorageService(s3Client, "bubli-test-bucket");

		FileUploadResult result = storageService.save(
				"resources/test-resource/v1.pdf",
				"contract.pdf",
				"application/pdf",
				new byte[]{1, 2, 3}
		);

		ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
		PutObjectRequest request = requestCaptor.getValue();

		assertThat(request.bucket()).isEqualTo("bubli-test-bucket");
		assertThat(request.key()).isEqualTo("resources/test-resource/v1.pdf");
		assertThat(request.contentType()).isEqualTo("application/pdf");
		assertThat(request.contentLength()).isEqualTo(3L);
		assertThat(result.storageKey()).isEqualTo("resources/test-resource/v1.pdf");
		assertThat(result.originalName()).isEqualTo("contract.pdf");
		assertThat(result.mimeType()).isEqualTo("application/pdf");
		assertThat(result.sizeBytes()).isEqualTo(3L);
		assertThat(result.checksum()).isEqualTo("039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81");
	}

	@Test
	void deleteRemovesObject() {
		given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
				.willReturn(DeleteObjectResponse.builder().build());
		S3StorageService storageService = new S3StorageService(s3Client, "bubli-test-bucket");

		storageService.delete("resources/test-resource/v1.pdf");

		ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
		verify(s3Client).deleteObject(requestCaptor.capture());
		DeleteObjectRequest request = requestCaptor.getValue();

		assertThat(request.bucket()).isEqualTo("bubli-test-bucket");
		assertThat(request.key()).isEqualTo("resources/test-resource/v1.pdf");
	}

	@Test
	void saveThrowsWhenBucketIsMissing() {
		S3StorageService storageService = new S3StorageService(s3Client, "");

		assertThatThrownBy(() -> storageService.save(
				"resources/test-resource/v1.pdf",
				"contract.pdf",
				"application/pdf",
				new byte[]{1, 2, 3}
		)).isInstanceOf(BusinessException.class);
	}

	@Test
	void saveThrowsWhenStorageKeyIsMissing() {
		S3StorageService storageService = new S3StorageService(s3Client, "bubli-test-bucket");

		assertThatThrownBy(() -> storageService.save(
				"",
				"contract.pdf",
				"application/pdf",
				new byte[]{1, 2, 3}
		)).isInstanceOf(BusinessException.class);
	}
}
