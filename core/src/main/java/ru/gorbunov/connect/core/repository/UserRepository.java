package ru.gorbunov.connect.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.models.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Boolean existsByEmail(String email);
    Boolean existsByUserName(String userName);
    Optional<User> findByUserName(String userName);
    List<User> findByUserNameStartingWith(String prefix);
    Optional<User> findByEmail(String email);
    List<User> findAllByRoles(Role role);
}
