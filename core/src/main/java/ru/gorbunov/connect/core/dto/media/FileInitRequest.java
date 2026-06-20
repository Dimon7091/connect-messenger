package ru.gorbunov.connect.core.dto.media;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FileInitRequest(
        @NotBlank String fileName,
        @NotBlank String mimeType,
        @NotNull @Positive Long fileSize
) { }
