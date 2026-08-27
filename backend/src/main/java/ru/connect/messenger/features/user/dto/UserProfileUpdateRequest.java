package ru.connect.messenger.features.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UserProfileUpdateRequest(
        @NotBlank(message = "Имя не может быть пустым")
        @Size(min = 2, max = 50, message = "Имя должно быть от 2 до 50 символов")
        @Pattern(
                regexp = "^[a-zA-Zа-яА-ЯёЁ]+$",
                message = "Имя может содержать только буквы русского или "
                        + "английского языка без пробелов и знаков препинания"
        )
        JsonNullable<String> firstName,

        @NotBlank(message = "Имя не может быть пустым")
        @Size(min = 2, max = 50, message = "Фамилия не может превышать 50 символов")
        @Pattern(
                regexp = "^[a-zA-Zа-яА-ЯёЁ]*$",
                message = "Фамилия может содержать только буквы русского или английского языка"
        )
        JsonNullable<String> lastName
) { }
