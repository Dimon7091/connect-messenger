package ru.gorbunov.connect.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.gorbunov.connect.core.models.UserStatus;

@Repository
public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {

}

