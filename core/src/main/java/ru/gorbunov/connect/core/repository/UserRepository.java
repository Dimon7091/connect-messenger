package ru.gorbunov.connect.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.models.User;

import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Boolean existsByEmail(String email);
    Boolean existsByUserName(String userName);
    Optional<User> findByUserName(String userName);
    @Query(value = "SELECT id FROM users WHERE user_name = :userName", nativeQuery = true)
    Optional<Long> findUserIdByUserName(@Param("userName") String userName);
    List<User> findByUserNameStartingWith(String prefix);
    Optional<User> findByEmail(String email);
    List<User> findAllByRoles(Role role);
    @Query(value = "SELECT CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')) "
            + "FROM users u WHERE u.id = :id", nativeQuery = true)
    Optional<String> findFullNameById(@Param("id") Long id);
}
