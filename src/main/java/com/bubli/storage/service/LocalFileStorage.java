package com.bubli.storage.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private final Path basePath;

    public LocalFileStorage(@Value("${storage.local.base-path:./local-storage}") String basePath) {
        this.basePath = Path.of(basePath).toAbsolutePath().normalize();
    }

    @Override
    public String store(String key, InputStream inputStream) {
        Path target = resolveSafely(key);
        try (inputStream) {
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target);
            return basePath.relativize(target).toString().replace('\\', '/');
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.RESOURCE_500_001);
        }
    }

    @Override
    public InputStream open(String storageKey) {
        try {
            return Files.newInputStream(resolveSafely(storageKey));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.RESOURCE_500_001);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveSafely(storageKey));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.RESOURCE_500_001);
        }
    }

    private Path resolveSafely(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("storage key는 필수입니다.");
        }

        Path resolved = basePath.resolve(key).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new IllegalArgumentException("허용되지 않은 storage key입니다.");
        }
        return resolved;
    }
}
