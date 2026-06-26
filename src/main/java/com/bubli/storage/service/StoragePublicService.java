package com.bubli.storage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class StoragePublicService {

    private final FileStorage fileStorage;

    public String store(String key, InputStream inputStream) {
        return fileStorage.store(key, inputStream);
    }

    public InputStream open(String storageKey) {
        return fileStorage.open(storageKey);
    }

    public void delete(String storageKey) {
        fileStorage.delete(storageKey);
    }
}
