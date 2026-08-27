package ru.connect.messenger.features.storage.dto;

public record FileInitResponse(
        String uploadUrl,
        String fileKey,
        String mimeType,
        String fileName,
        String size
) { }
