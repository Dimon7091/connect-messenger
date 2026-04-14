package ru.gorbunov.connect.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gorbunov.connect.core.models.ChatParticipant;
import ru.gorbunov.connect.core.models.ChatParticipantId;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, ChatParticipantId> {
}
