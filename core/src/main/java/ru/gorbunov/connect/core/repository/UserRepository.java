package ru.gorbunov.connect.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.models.User;

import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Boolean existsByEmail(String email);
    Boolean existsByUserName(String userName);
    Optional<User> findByUserName(String userName);
    List<User> findByUserNameStartingWith(String prefix);
    Optional<User> findByEmail(String email);
    List<User> findAllByRoles(Role role);
    @Query("SELECT CONCAT(u.profile.firstName, ' ', u.profile.lastName) FROM User u WHERE u.id = :id")
    Optional<String> findFullNameById(@Param("id") Long id);
}
