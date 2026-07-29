package ru.gorbunov.connect.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.models.User;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Boolean existsByUserName(String userName);
    @Query(value = "SELECT isBanned FROM User WHERE id = :id")
    Boolean isBanned(@Param("id") Long id);
    @Query(value = "SELECT isDeleted FROM User WHERE id = :id")
    Boolean isDeleted(@Param("id") Long id);
    Optional<User> findByUserName(String userName);
    @Query(value = "SELECT id FROM User WHERE userName = :userName")
    Optional<Long> findUserIdByUserName(@Param("userName") String userName);
    List<User> findByUserNameStartingWith(String prefix);
    List<User> findAllByRoles(Role role);
    @Query(value = "SELECT CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')) "
            + "FROM users u WHERE u.id = :id", nativeQuery = true)
    Optional<String> findFullNameById(@Param("id") Long id);
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE User SET isBanned = :flag WHERE id = :id")
    void updateIsBanned(@Param("id") Long id, @Param("flag") boolean flag);
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END "
            + "FROM User u JOIN u.roles r "
            + "WHERE u.id = :id AND r = ru.gorbunov.connect.core.models.Role.ROLE_ADMIN AND u.isDeleted = false")
    boolean isAdmin(@Param("id") Long userId);
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END "
            + "FROM User u JOIN u.roles r "
            + "WHERE u.userName = :userName AND r = ru.gorbunov.connect.core.models.Role.ROLE_ADMIN AND u.isDeleted = false")
    boolean isAdmin(@Param("userName") String userName);
}
