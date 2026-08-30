package ru.connect.messenger.core;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class SecurityUser implements UserDetails {
    private final Long id; // 🟢 Наш ID
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public SecurityUser(Long id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = List.of(new SimpleGrantedAuthority(role));
    }

    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return password; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
}
