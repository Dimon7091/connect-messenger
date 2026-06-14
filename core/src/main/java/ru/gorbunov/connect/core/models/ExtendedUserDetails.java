package ru.gorbunov.connect.core.models;

import org.springframework.security.core.userdetails.UserDetails;

public interface ExtendedUserDetails extends UserDetails {
    Long getId();
}
