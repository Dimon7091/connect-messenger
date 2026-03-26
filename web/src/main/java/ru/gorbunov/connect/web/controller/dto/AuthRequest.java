package ru.gorbunov.connect.web.controller.dto;

public record AuthRequest(
        String userName,
        String password
) {
}
