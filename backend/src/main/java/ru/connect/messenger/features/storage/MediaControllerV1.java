package ru.connect.messenger.features.storage;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.connect.messenger.features.messaging.chat.service.ChatFileService;
import ru.connect.messenger.features.storage.dto.FileDownloadUrlRequest;
import ru.connect.messenger.features.storage.dto.FileDownloadUrlResponse;
import ru.connect.messenger.features.storage.dto.FileInitRequest;
import ru.connect.messenger.features.storage.dto.FileInitResponse;

@RestController
@RequestMapping("api/v1/media")
public class MediaControllerV1 {
    private final ChatFileService chatFileService;

    public MediaControllerV1(ChatFileService chatFileService) {
        this.chatFileService = chatFileService;
    }

    @PostMapping("/upload-url")
    public ResponseEntity<FileInitResponse> getUploadUrl(@RequestBody FileInitRequest request) {
        var response = chatFileService.getFileUploadUrl(
                request.fileName(),
                request.mimeType(),
                request.fileSize()
        );
        return ResponseEntity.ok()
                .body(response);
    }

    @PostMapping("/download-url")
    public ResponseEntity<FileDownloadUrlResponse> getDownloadUrl(@RequestBody FileDownloadUrlRequest request) {
        var response = chatFileService.getFileDownloadUrl(request.fileKey());
        return ResponseEntity.ok()
                .body(response);
    }
}
