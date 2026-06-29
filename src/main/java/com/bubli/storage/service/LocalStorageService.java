package com.bubli.storage.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.storage.dto.FileUploadResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
public class LocalStorageService implements StoragePublicService {

	@Value("${storage.local.base-path}")
	private String basePath;

	@Override
	public FileUploadResult save(String storageKey, String originalName, String mimeType, byte[] content) {
		if (content == null) {
			throw new BusinessException(ErrorCode.RESOURCE_400_001);
		}
		Path target = resolveSafely(storageKey);
		try {
			Files.createDirectories(target.getParent());
			Files.write(target, content);
		} catch (IOException e) {
			throw new BusinessException(ErrorCode.RESOURCE_500_001);
		}
		return new FileUploadResult(storageKey, originalName, mimeType, content.length, checksum(content));
	}

	@Override
	public InputStream open(String storageKey) {
		try {
			return Files.newInputStream(resolveSafely(storageKey));
		} catch (IOException e) {
			throw new BusinessException(ErrorCode.RESOURCE_500_001);
		}
	}

	@Override
	public void delete(String storageKey) {
		try {
			Files.deleteIfExists(resolveSafely(storageKey));
		} catch (IOException e) {
			throw new BusinessException(ErrorCode.RESOURCE_500_001);
		}
	}

	public String generateDownloadUrl(String storageKey) {
		return resolveSafely(storageKey).toUri().toString();
	}

	private Path resolveSafely(String storageKey) {
		if (storageKey == null || storageKey.isBlank()) {
			throw new BusinessException(ErrorCode.RESOURCE_400_001);
		}
		Path root = Path.of(basePath).toAbsolutePath().normalize();
		Path resolved = root.resolve(storageKey).normalize();
		if (!resolved.startsWith(root)) {
			throw new BusinessException(ErrorCode.RESOURCE_400_001);
		}
		return resolved;
	}

	private String checksum(byte[] content) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(content);
			StringBuilder sb = new StringBuilder();
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
