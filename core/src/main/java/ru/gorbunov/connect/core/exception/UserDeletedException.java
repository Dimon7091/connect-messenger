package ru.gorbunov.connect.core.exception;

public class UserDeletedException extends RuntimeException {
    public UserDeletedException(String message) {
        super(message);
    }
}
