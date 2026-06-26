package com.bubli.storage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
public class LocalStorageService implements StorageService {

	@Value("${storage.local.base-path}")
	private String basePath;

	@Override
	public String generateDownloadUrl(String storageKey) {
		return "file://" + basePath + "/" + storageKey;
	}
}
