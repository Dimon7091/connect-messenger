package ru.connect.messenger.features.user.domain;

public record UserPrincipal(String name) implements java.security.Principal {
    @Override public String getName() {
        return name;
    }
}
