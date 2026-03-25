package ru.gorbunov.connect.core.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserPutUpdateRequest(
        @Email(
                regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                message = "Не правильный формат email"
        )
        @NotBlank(message = "email не может быть пустым")
        String email,

        @NotBlank(message = "Имя пользователя не может быть пустым")
        @Size(max = 50, message = "Имя пользователя не может привышать 50 символов")
        @Pattern(
                regexp = "^[a-zA-Z0-9]+$\n",
                message = "Имя пользователя, может содержать английские буквы в ниженем или верхнем регистре и цыфры"
        )
        String userName,

        @NotBlank(message = "Имя не может быть пустым")
        @Size(max = 50, message = "Имя не может привышать 50 символов")
        @Pattern(
                regexp = "^[a-zA-Zа-яА-ЯёЁ]+$\n",
                message = "Имя может содержать только буквы русского или английского языка"
        )
        String firstName,

        @NotBlank(message = "Фамилия не может быть пустым")
        @Size(max = 50, message = "Фамилия не может привышать 50 символов")
        @Pattern(
                regexp = "^[a-zA-Zа-яА-ЯёЁ]+$\n",
                message = "Фамилия может содержать только буквы русского или английского языка"
        )
        String lastName
) { }
