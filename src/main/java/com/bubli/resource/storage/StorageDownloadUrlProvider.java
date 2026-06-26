package com.bubli.resource.storage;

public interface StorageDownloadUrlProvider {

	StorageDownloadUrl issueDownloadUrl(String storageKey, String originalName);
}
