package ru.gorbunov.connect.core.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UserPatchUpdateRequest(
        @Email(
                regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                message = "Неправильный формат email"
        )
        // Для PATCH лучше убрать @NotBlank, если поле может не передаваться вовсе.
        // Если поле передано как null, JsonNullable это обработает.
        JsonNullable<String> email,

        @Size(max = 50, message = "Имя пользователя не может превышать 50 символов")
        @Pattern(
                regexp = "^[a-zA-Z0-9]+$", // Убран \n
                message = "Имя пользователя может содержать только английские буквы и цифры"
        )
        JsonNullable<String> userName,

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
) {}

