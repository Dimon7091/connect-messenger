package ru.gorbunov.connect.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank(message = "Имя пользователя не может быть пустым")
        String username,
        @NotBlank(message = "Пароль не может быть пустым")
        String password
) {
}
