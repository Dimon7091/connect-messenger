package ru.gorbunov.connect.core.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UserProfileUpdateRequest(
        @Size(max = 50, message = "Имя не может превышать 50 символов")
        @Pattern(
                regexp = "^[a-zA-Zа-яА-ЯёЁ]+$", // Убран \n
                message = "Имя может содержать только буквы русского или английского языка"
        )
        JsonNullable<String> firstName,

        @Size(max = 50, message = "Фамилия не может превышать 50 символов")
        @Pattern(
                regexp = "^[a-zA-Zа-яА-ЯёЁ]+$", // Убран \n
                message = "Фамилия может содержать только буквы русского или английского языка"
        )
        JsonNullable<String> lastName
) { }
