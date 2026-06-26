package com.bubli.storage.service;

import com.bubli.storage.dto.FileUploadResult;

public interface StorageService {

	FileUploadResult save(String storageKey, String originalName, String mimeType, byte[] content);

	void delete(String storageKey);
}
