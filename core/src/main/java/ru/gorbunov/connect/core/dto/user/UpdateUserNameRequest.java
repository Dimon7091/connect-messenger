package ru.gorbunov.connect.core.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.gorbunov.connect.core.dto.user.validation.UniqueUserName;

public record UpdateUserNameRequest(
        @JsonProperty("username")
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
        String userName
) { }
