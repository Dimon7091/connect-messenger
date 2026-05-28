package ru.gorbunov.connect.core.models;

import java.io.InputStream;

public interface FileStorageProvider {
    void upload(String key, byte[] content, String contentType, StorageType type);
    InputStream download(String key, StorageType type);
    String generatePresignedUrl(String key, StorageType type, long durationInMinutes);
    void delete(String key, StorageType type);
}
