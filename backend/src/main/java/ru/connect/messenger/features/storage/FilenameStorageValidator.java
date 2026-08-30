package ru.connect.messenger.features.storage;

public interface FilenameStorageValidator {
    String generateSafeS3Key(String originalFilename);
}
