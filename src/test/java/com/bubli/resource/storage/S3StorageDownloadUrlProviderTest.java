package com.bubli.resource.storage;

import com.bubli.global.error.BusinessException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3StorageDownloadUrlProviderTest {

	@Test
	void issueDownloadUrlCreatesPresignedS3UrlWithoutCallingS3() {
		Instant before = Instant.now();
		try (S3Presigner presigner = S3Presigner.builder()
				.region(Region.AP_NORTHEAST_2)
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create("test-access-key", "test-secret-key")
				))
				.build()) {
			S3StorageDownloadUrlProvider provider = new S3StorageDownloadUrlProvider(
					presigner,
					"bubli-test-bucket",
					300
			);

			StorageDownloadUrl result = provider.issueDownloadUrl(
					"resources/test-resource/v1.pdf",
					"계약서 v1.pdf"
			);

			assertThat(result.url()).contains("X-Amz-Signature");
			assertThat(result.url()).contains("X-Amz-Expires=300");
			assertThat(result.url()).contains("resources/test-resource/v1.pdf");
			assertThat(result.expiresAt()).isAfterOrEqualTo(before.plusSeconds(299));
			assertThat(result.expiresAt()).isBeforeOrEqualTo(Instant.now().plusSeconds(301));
		}
	}

	@Test
	void issueDownloadUrlThrowsWhenBucketIsMissing() {
		try (S3Presigner presigner = S3Presigner.builder()
				.region(Region.AP_NORTHEAST_2)
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create("test-access-key", "test-secret-key")
				))
				.build()) {
			S3StorageDownloadUrlProvider provider = new S3StorageDownloadUrlProvider(
					presigner,
					"",
					300
			);

			assertThatThrownBy(() -> provider.issueDownloadUrl("resources/test-resource/v1.pdf", "계약서.pdf"))
					.isInstanceOf(BusinessException.class);
		}
	}
}
