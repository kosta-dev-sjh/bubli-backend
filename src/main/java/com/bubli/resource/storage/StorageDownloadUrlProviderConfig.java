package com.bubli.resource.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageDownloadUrlProviderConfig {

	@Bean
	@ConditionalOnMissingBean(StorageDownloadUrlProvider.class)
	public StorageDownloadUrlProvider disabledStorageDownloadUrlProvider() {
		return new DisabledStorageDownloadUrlProvider();
	}
}
