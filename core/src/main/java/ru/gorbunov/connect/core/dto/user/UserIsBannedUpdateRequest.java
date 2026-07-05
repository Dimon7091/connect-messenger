package ru.gorbunov.connect.core.dto.user;

import jakarta.validation.constraints.NotNull;

public record UserIsBannedUpdateRequest(
        @NotNull(message = "Флаг бана не может быть null")
        Boolean isBanned
) { }
