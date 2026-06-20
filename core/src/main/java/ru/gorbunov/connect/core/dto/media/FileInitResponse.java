package ru.gorbunov.connect.core.dto.media;

public record FileInitResponse(
        String uploadUrl,
        String fileKey,
        String mimeType,
        String fileName,
        String size
) { }
