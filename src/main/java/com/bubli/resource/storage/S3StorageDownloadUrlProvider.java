package com.bubli.resource.storage;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3StorageDownloadUrlProvider implements StorageDownloadUrlProvider {

	private final S3Presigner s3Presigner;
	private final String bucket;
	private final Duration downloadUrlExpiresIn;

	public S3StorageDownloadUrlProvider(
			S3Presigner s3Presigner,
			@Value("${aws.s3.bucket:}") String bucket,
			@Value("${storage.s3.download-url-expire-seconds:600}") long downloadUrlExpireSeconds
	) {
		this.s3Presigner = s3Presigner;
		this.bucket = bucket;
		this.downloadUrlExpiresIn = Duration.ofSeconds(downloadUrlExpireSeconds);
	}

	@Override
	public StorageDownloadUrl issueDownloadUrl(String storageKey, String originalName) {
		if (!StringUtils.hasText(bucket) || !StringUtils.hasText(storageKey)) {
			throw new BusinessException(ErrorCode.RESOURCE_501_001);
		}
		GetObjectRequest.Builder getObjectRequestBuilder = GetObjectRequest.builder()
				.bucket(bucket)
				.key(storageKey);
		if (StringUtils.hasText(originalName)) {
			getObjectRequestBuilder.responseContentDisposition(contentDisposition(originalName));
		}
		PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
				.signatureDuration(downloadUrlExpiresIn)
				.getObjectRequest(getObjectRequestBuilder.build())
				.build());
		return new StorageDownloadUrl(
				presignedRequest.url().toString(),
				presignedRequest.expiration()
		);
	}

	private String contentDisposition(String originalName) {
		String encodedName = URLEncoder.encode(originalName, StandardCharsets.UTF_8)
				.replace("+", "%20");
		return "attachment; filename*=UTF-8''" + encodedName;
	}
}
