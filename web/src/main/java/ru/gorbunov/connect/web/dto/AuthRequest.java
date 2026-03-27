package ru.gorbunov.connect.web.dto;

public record AuthRequest(
        String userName,
        String password
) {
}
