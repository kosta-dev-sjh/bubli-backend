package com.bubli.storage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void storesAndDeletesFileUnderBasePath() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(tempDir.toString());

        String storedPath = storage.store(
                "documents/room/sample.txt",
                new ByteArrayInputStream("sample".getBytes(StandardCharsets.UTF_8))
        );

        Path file = tempDir.resolve(storedPath);
        assertThat(Files.readString(file)).isEqualTo("sample");

        storage.delete(storedPath);
        assertThat(file).doesNotExist();
    }

    @Test
    void rejectsPathTraversal() {
        LocalFileStorage storage = new LocalFileStorage(tempDir.toString());

        assertThatThrownBy(() -> storage.store(
                "../outside.txt",
                new ByteArrayInputStream(new byte[0])
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
