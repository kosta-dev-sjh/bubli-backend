package com.bubli.storage.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.storage.dto.FileUploadResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

	private final S3Client s3Client;
	private final String bucket;

	public S3StorageService(
			S3Client s3Client,
			@Value("${aws.s3.bucket:}") String bucket
	) {
		this.s3Client = s3Client;
		this.bucket = bucket;
	}

	@Override
	public FileUploadResult save(String storageKey, String originalName, String mimeType, byte[] content) {
		validateBucket();
		validateStorageKey(storageKey);
		if (content == null) {
			throw new BusinessException(ErrorCode.RESOURCE_400_001);
		}

		PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
				.bucket(bucket)
				.key(storageKey)
				.contentLength((long) content.length);
		if (StringUtils.hasText(mimeType)) {
			requestBuilder.contentType(mimeType);
		}

		s3Client.putObject(requestBuilder.build(), RequestBody.fromBytes(content));
		return new FileUploadResult(
				storageKey,
				originalName,
				mimeType,
				content.length,
				sha256(content)
		);
	}

	@Override
	public void delete(String storageKey) {
		validateBucket();
		validateStorageKey(storageKey);
		s3Client.deleteObject(DeleteObjectRequest.builder()
				.bucket(bucket)
				.key(storageKey)
				.build());
	}

	private void validateBucket() {
		if (!StringUtils.hasText(bucket)) {
			throw new BusinessException(ErrorCode.RESOURCE_501_002);
		}
	}

	private void validateStorageKey(String storageKey) {
		if (!StringUtils.hasText(storageKey)) {
			throw new BusinessException(ErrorCode.RESOURCE_400_001);
		}
	}

	private String sha256(byte[] content) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm is not available", e);
		}
	}
}
