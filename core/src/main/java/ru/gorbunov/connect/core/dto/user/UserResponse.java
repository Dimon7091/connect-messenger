package ru.gorbunov.connect.core.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import ru.gorbunov.connect.core.models.ExtendedUserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class UserResponse implements ExtendedUserDetails {
    private Long id;
    @JsonProperty("userName")
    private String userName;
    private String email;
    private Set<String> roles;
    private String createdAt;
    private String lastSeen;
    private ProfileResponse profile;
    private Boolean isDeleted;
    private Boolean isBanned;


    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return this.userName;
    }
}
