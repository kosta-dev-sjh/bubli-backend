package com.bubli.storage.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.storage.dto.FileUploadResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(StorageService.class)
public class DisabledStorageService implements StorageService {

	@Override
	public FileUploadResult save(String storageKey, String originalName, String mimeType, byte[] content) {
		throw new BusinessException(ErrorCode.RESOURCE_501_002);
	}

	@Override
	public void delete(String storageKey) {
		throw new BusinessException(ErrorCode.RESOURCE_501_002);
	}
}
