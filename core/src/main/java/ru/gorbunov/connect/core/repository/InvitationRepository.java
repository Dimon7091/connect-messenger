package ru.gorbunov.connect.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.gorbunov.connect.core.models.Invitation;

import java.time.LocalDateTime;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    long count();
    Optional<Invitation> findInvitationsByToken(String token);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Invitation i WHERE i.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);
}
