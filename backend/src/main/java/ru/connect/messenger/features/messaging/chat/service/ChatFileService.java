package ru.connect.messenger.features.messaging.chat.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.connect.messenger.features.storage.FileStorageProvider;
import ru.connect.messenger.shared.domain.StorageType;
import ru.connect.messenger.features.storage.FilenameStorageValidator;
import ru.connect.messenger.shared.dto.FileDownloadUrlResponse;
import ru.connect.messenger.shared.dto.FileInitResponse;

@AllArgsConstructor
@Service
public class ChatFileService {
    private final FileStorageProvider fileStorageProvider;
    private final FilenameStorageValidator filenameStorageValidator;
    private static final Long URL_EXPIRATION_MINUTES = 60L;

    public FileInitResponse getFileUploadUrl(String originalFileName, String mimeType, Long fileSize) {
       String fileKey = filenameStorageValidator.generateSafeS3Key(originalFileName);

       String uploadUrl = fileStorageProvider.generatePresignedUploadUrl(
               fileKey, StorageType.CHAT_FILE,
               URL_EXPIRATION_MINUTES,
               mimeType
       );
       return new FileInitResponse(uploadUrl, fileKey, mimeType, originalFileName, String.valueOf(fileSize));
    }

    public FileDownloadUrlResponse getFileDownloadUrl(String fileKey) {
        var downloadUrl = fileStorageProvider.generatePresignedDownloadUrl(
                fileKey,
                StorageType.CHAT_FILE,
                URL_EXPIRATION_MINUTES
        );
        return new FileDownloadUrlResponse(downloadUrl);
    }
}
