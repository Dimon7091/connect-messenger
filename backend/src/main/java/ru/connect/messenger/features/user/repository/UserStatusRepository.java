package ru.connect.messenger.features.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.connect.messenger.features.userstatus.UserStatus;

@Repository
public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {

}

