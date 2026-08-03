package ru.gorbunov.connect.core.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserNameRequest(
        @JsonProperty("username")
        @Size(max = 50, message = "Имя пользователя не может превышать 50 символов")
        @Pattern(
                regexp = "^[a-zA-Z0-9]+$", // Убран \n
                message = "Имя пользователя может содержать только английские буквы и цифры"
        )
        String userName
) { }
