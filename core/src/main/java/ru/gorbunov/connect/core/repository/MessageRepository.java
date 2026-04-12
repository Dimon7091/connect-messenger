package ru.gorbunov.connect.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gorbunov.connect.core.models.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
}
