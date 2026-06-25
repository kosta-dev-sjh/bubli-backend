package com.bubli.storage.service;

import java.io.InputStream;

public interface FileStorage {

    String store(String key, InputStream inputStream);

    InputStream open(String storagePath);

    void delete(String storagePath);
}
