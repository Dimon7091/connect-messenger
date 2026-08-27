package ru.connect.messenger.features.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.connect.messenger.features.user.validation.UniqueUserName;

public record UserCreateRequest(
        @NotBlank(message = "Имя пользователя не может быть пустым")
        @Size(min = 3, max = 20, message = "Имя пользователя должно быть от 3 до 20 символов")
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "Имя пользователя может содержать только английские буквы, цифры, дефис, подчеркивание, точку"
        )
        @Pattern(
                regexp = "^(?!^[0-9]+$)(?!^\\.+$).*$",
                message = "Имя пользователя не может состоять только из цифр или только из точек"
        )
        @UniqueUserName(message = "Имя пользователя уже занято")
        String userName,

        @NotBlank(message = "Имя не может быть пустым")
        @Size(min = 2, max = 50, message = "Имя должно быть от 2 до 50 символов")
        @Pattern(
                regexp = "^[a-zA-Zа-яА-ЯёЁ]+$",
                message = "Имя может содержать только буквы русского или "
                        + "английского языка без пробелов и знаков препинания"
        )
        String firstName,

        @Size(min = 2, max = 50, message = "Фамилия не может превышать 50 символов")
        @Pattern(
                regexp = "^[a-zA-Zа-яА-ЯёЁ]*$",
                message = "Фамилия может содержать только буквы русского или английского языка"
        )
        String lastName,

        @NotBlank(message = "Пароль не может быть пустым")
        @Size(min = 8, max = 50, message = "Пароль должен быть от 8 до 50 символов")
        String password,

        @NotBlank(message = "Отсутствует приглашение")
        String invitationToken
) { }
