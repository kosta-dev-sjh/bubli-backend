package com.bubli.storage.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.storage.dto.FileUploadResult;

import java.io.IOException;
import java.io.InputStream;

public interface StoragePublicService extends StorageService {

    default String store(String key, InputStream inputStream) {
        try (inputStream) {
            return save(key, null, null, inputStream.readAllBytes()).storageKey();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.RESOURCE_500_001);
        }
    }

    default InputStream open(String storageKey) {
        throw new BusinessException(ErrorCode.RESOURCE_501_002);
    }

    @Override
    FileUploadResult save(String storageKey, String originalName, String mimeType, byte[] content);

    @Override
    void delete(String storageKey);
}
