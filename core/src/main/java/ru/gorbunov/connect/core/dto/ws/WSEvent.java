package ru.gorbunov.connect.core.dto.ws;

public class WSEvent<T> {
    private String type;
    private T payload;
}