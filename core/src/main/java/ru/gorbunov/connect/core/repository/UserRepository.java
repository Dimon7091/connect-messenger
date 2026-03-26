package ru.gorbunov.connect.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gorbunov.connect.core.models.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Boolean existsByEmail(String email);
    Boolean existsByUserName(String userName);
}
