package ru.gorbunov.connect.core.service;

import org.springframework.stereotype.Service;
import ru.gorbunov.connect.core.dto.media.FileDownloadUrlResponse;
import ru.gorbunov.connect.core.dto.media.FileInitResponse;
import ru.gorbunov.connect.core.models.FileStorageProvider;
import ru.gorbunov.connect.core.models.StorageType;
import ru.gorbunov.connect.core.util.S3FilenameValidator;

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
