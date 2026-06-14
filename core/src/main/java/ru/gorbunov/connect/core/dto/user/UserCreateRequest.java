package ru.gorbunov.connect.core.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @Email(
                regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                message = "Неправильный формат email"
        )
        @NotBlank(message = "Email не может быть пустым")
        String email,

        @NotBlank(message = "Имя пользователя не может быть пустым")
        @Size(max = 50, message = "Имя пользователя не может превышать 50 символов")
        @Pattern(
                regexp = "^[a-zA-Z0-9]+$",
                message = "Имя пользователя может содержать только английские буквы и цифры"
        )
        String userName,

        @NotBlank(message = "Имя не может быть пустым")
        @Size(max = 50, message = "Имя не может превышать 50 символов")
        @Pattern(
                regexp = "^[a-zA-Zа-яА-ЯёЁ]+$",
                message = "Имя может содержать только буквы русского или английского языка"
        )
        String firstName,

        @NotBlank(message = "Фамилия не может быть пустой")
        @Size(max = 50, message = "Фамилия не может превышать 50 символов")
        @Pattern(
                regexp = "^[a-zA-Zа-яА-ЯёЁ]+$",
                message = "Фамилия может содержать только буквы русского или английского языка"
        )
        String lastName,

        @NotBlank(message = "Пароль не может быть пустым")
        @Size(min = 8, max = 50, message = "Пароль должен быть от 8 до 50 символов")
        String password
) { }
