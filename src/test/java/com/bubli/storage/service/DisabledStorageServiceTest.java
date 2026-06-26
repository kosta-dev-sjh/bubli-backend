package com.bubli.storage.service;

import com.bubli.global.error.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisabledStorageServiceTest {

	private final DisabledStorageService storageService = new DisabledStorageService();

	@Test
	void saveThrowsWhenStorageIsNotConfigured() {
		assertThatThrownBy(() -> storageService.save(
				"resources/test-resource/v1.pdf",
				"contract.pdf",
				"application/pdf",
				new byte[]{1, 2, 3}
		)).isInstanceOf(BusinessException.class);
	}

	@Test
	void deleteThrowsWhenStorageIsNotConfigured() {
		assertThatThrownBy(() -> storageService.delete("resources/test-resource/v1.pdf"))
				.isInstanceOf(BusinessException.class);
	}
}
