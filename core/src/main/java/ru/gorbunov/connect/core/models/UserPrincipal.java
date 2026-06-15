package ru.gorbunov.connect.core.models;

public record UserPrincipal(String name) implements java.security.Principal {
    @Override public String getName() {
        return name;
    }
}
