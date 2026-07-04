package com.bubli.storage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStorageServiceTest {

	@TempDir
	Path tempDir;

	@Test
	void existsChecksFileUnderBasePath() throws Exception {
		LocalStorageService storageService = new LocalStorageService();
		ReflectionTestUtils.setField(storageService, "basePath", tempDir.toString());
		Path file = tempDir.resolve("resources/test-resource/v1.txt");
		Files.createDirectories(file.getParent());
		Files.writeString(file, "body");

		assertThat(storageService.exists("resources/test-resource/v1.txt")).isTrue();
		assertThat(storageService.exists("resources/test-resource/missing.txt")).isFalse();
	}
}
