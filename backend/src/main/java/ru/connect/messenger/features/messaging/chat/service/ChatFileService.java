package ru.connect.messenger.features.messaging.chat.service;

import org.springframework.stereotype.Service;
import ru.connect.messenger.features.storage.FileStorageProvider;
import ru.connect.messenger.features.storage.S3FilenameValidator;
import ru.connect.messenger.features.storage.StorageType;
import ru.connect.messenger.features.storage.dto.FileDownloadUrlResponse;
import ru.connect.messenger.features.storage.dto.FileInitResponse;

@Service
public class ChatFileService {
    private final FileStorageProvider storageProvider;
    private static final Long URL_EXPIRATION_MINUTES = 60L;

    public ChatFileService(FileStorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    public FileInitResponse getFileUploadUrl(String originalFileName, String mimeType, Long fileSize) {
       String fileKey = S3FilenameValidator.generateSafeS3Key(originalFileName);

       String uploadUrl = storageProvider.generatePresignedUploadUrl(
               fileKey, StorageType.CHAT_FILE,
               URL_EXPIRATION_MINUTES,
               mimeType
       );
       return new FileInitResponse(uploadUrl, fileKey, mimeType, originalFileName, String.valueOf(fileSize));
    }

    public FileDownloadUrlResponse getFileDownloadUrl(String fileKey) {
        var downloadUrl = storageProvider.generatePresignedDownloadUrl(
                fileKey,
                StorageType.CHAT_FILE,
                URL_EXPIRATION_MINUTES
        );
        return new FileDownloadUrlResponse(downloadUrl);
    }
}
