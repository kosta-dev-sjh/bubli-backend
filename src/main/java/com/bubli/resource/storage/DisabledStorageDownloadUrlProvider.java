package com.bubli.resource.storage;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;

public class DisabledStorageDownloadUrlProvider implements StorageDownloadUrlProvider {

	@Override
	public StorageDownloadUrl issueDownloadUrl(String storageKey, String originalName) {
		throw new BusinessException(ErrorCode.RESOURCE_501_001);
	}
}
