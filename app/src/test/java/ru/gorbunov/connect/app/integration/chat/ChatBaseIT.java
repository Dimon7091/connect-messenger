package ru.gorbunov.connect.app.integration.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.gorbunov.connect.core.models.Chat;
import ru.gorbunov.connect.core.models.ChatParticipant;
import ru.gorbunov.connect.core.models.ChatParticipantId;
import ru.gorbunov.connect.core.models.User;
import ru.gorbunov.connect.core.repository.ChatRepository;
import ru.gorbunov.connect.core.service.BanService;
import ru.gorbunov.connect.core.service.ChatService;
import ru.gorbunov.connect.core.service.UserService;
import ru.gorbunov.connect.core.service.orchestrators.UserDeletionService;
import ru.gorbunov.connect.web.util.JwtUtil;

import java.time.OffsetDateTime;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ChatBaseIT {
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected UserService userService;
    @Autowired
    protected JwtUtil jwtUtil;
    @Autowired
    protected ChatRepository chatRepository;
    @Autowired
    protected UserDeletionService userDeletionService;
    @Autowired
    protected BanService banService;
    @Autowired
    protected ChatService chatService;
    @Autowired
    protected jakarta.persistence.EntityManager entityManager;

    protected Chat createChat(User userA, User userB) {
        String directKey = Math.min(userA.getId(), userB.getId())
                + ":" + Math.max(userA.getId(), userB.getId());

        Chat newChat = new Chat();
        newChat.setDirectKey(directKey);
        newChat.setType("DIRECT");
        newChat.setCreatedAt(OffsetDateTime.now());
        Chat savedChat = chatRepository.save(newChat);

        // Создание первого участника чата
        var chatParticipantId1 = new ChatParticipantId(savedChat.getId(), userA.getId());
        var chatParticipant1 = new ChatParticipant();
        chatParticipant1.setChat(savedChat);
        chatParticipant1.setId(chatParticipantId1);
        chatParticipant1.setIsDeleted(false);
        chatParticipant1.setIsMuted(false);
        chatParticipant1.setIsChatEmpty(true);

        // Создание второго участника чата
        var chatParticipantId2 = new ChatParticipantId(savedChat.getId(), userB.getId());
        var chatParticipant2 = new ChatParticipant();
        chatParticipant2.setChat(savedChat);
        chatParticipant2.setId(chatParticipantId2);
        chatParticipant2.setIsDeleted(false);
        chatParticipant2.setIsMuted(false);
        chatParticipant2.setIsChatEmpty(true);

        savedChat.addParticipant(chatParticipant1);
        savedChat.addParticipant(chatParticipant2);
        return savedChat;
    }
}
