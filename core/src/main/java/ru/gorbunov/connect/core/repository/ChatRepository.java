package ru.gorbunov.connect.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gorbunov.connect.core.models.Chat;

public interface ChatRepository extends JpaRepository<Chat, Long> {
}
