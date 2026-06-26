package com.bubli.storage.service;

import com.bubli.storage.dto.FileUploadResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
public class LocalStorageService implements StorageService {

	@Value("${storage.local.base-path}")
	private String basePath;

	@Override
	public FileUploadResult save(String storageKey, String originalName, String mimeType, byte[] content) {
		Path target = Path.of(basePath, storageKey);
		try {
			Files.createDirectories(target.getParent());
			Files.write(target, content);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return new FileUploadResult(storageKey, originalName, mimeType, content.length, checksum(content));
	}

	@Override
	public void delete(String storageKey) {
		try {
			Files.deleteIfExists(Path.of(basePath, storageKey));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public String generateDownloadUrl(String storageKey) {
		return "file://" + basePath + "/" + storageKey;
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
