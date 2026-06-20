package ru.gorbunov.connect.core.models;

import java.io.InputStream;

public interface FileStorageProvider {
    void upload(String key, byte[] content, String contentType, StorageType type);
    InputStream download(String key, StorageType type);
    String generatePresignedDownloadUrl(String key, StorageType type, long durationInMinutes);
    String generatePresignedUploadUrl(String key, StorageType type, long durationInMinutes, String contentType);
    void delete(String key, StorageType type);
}
