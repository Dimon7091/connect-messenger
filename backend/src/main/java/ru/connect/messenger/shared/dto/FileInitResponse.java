package ru.connect.messenger.shared.dto;

public record FileInitResponse(
        String uploadUrl,
        String fileKey,
        String mimeType,
        String fileName,
        String size
) { }
