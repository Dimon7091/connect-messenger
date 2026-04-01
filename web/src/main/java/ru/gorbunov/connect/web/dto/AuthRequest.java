package ru.gorbunov.connect.web.dto;

public record AuthRequest(
        String username,
        String password
) {
}
