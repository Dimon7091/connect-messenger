package ru.connect.messenger.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.connect.messenger.core.JwtTokenProvider;
import ru.connect.messenger.features.invite.InviteServiceImpl;
import ru.connect.messenger.features.messaging.chat.domain.Chat;
import ru.connect.messenger.features.messaging.chat.domain.ChatParticipant;
import ru.connect.messenger.features.messaging.chat.domain.ChatParticipantId;
import ru.connect.messenger.features.messaging.chat.repository.ChatRepository;
import ru.connect.messenger.features.messaging.chat.service.ChatServiceImpl;
import ru.connect.messenger.features.messaging.message.domain.Message;
import ru.connect.messenger.features.messaging.message.repository.MessageRepository;
import ru.connect.messenger.features.user.domain.User;
import ru.connect.messenger.features.user.repository.UserRepository;
import ru.connect.messenger.features.user.service.UserBanService;
import ru.connect.messenger.features.user.service.UserBlockService;
import ru.connect.messenger.features.user.service.UserDeletionOrchestrator;
import ru.connect.messenger.features.user.service.UserServiceImpl;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Arrays;

@SpringBootTest
@AutoConfigureMockMvc
@Getter
@ActiveProfiles("test")
public class BaseIT {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserServiceImpl userServiceImpl;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InviteServiceImpl inviteServiceImpl;
    @Autowired
    private UserBanService userBanService;
    @Autowired
    private UserDeletionOrchestrator userDeletionOrchestrator;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private ChatServiceImpl chatService;
    @Autowired
    private jakarta.persistence.EntityManager entityManager;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private UserBlockService userBlockService;

    // Вспомогательные методы
    protected String createInvitationToken() throws MalformedURLException {
        URL uri;
        uri = new URL(inviteServiceImpl.create().invitationUrl());
        String query = uri.getQuery();
        return Arrays.stream(query.split("&"))
                .map(param -> param.split("="))
                .filter(pair -> pair.length > 1 && pair[0].equals("invitationToken"))
                .map(pair -> pair[1])
                .findFirst()
                .orElse(null);
    }

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
        chatParticipant1.setUnreadCount(0);

        // Создание второго участника чата
        var chatParticipantId2 = new ChatParticipantId(savedChat.getId(), userB.getId());
        var chatParticipant2 = new ChatParticipant();
        chatParticipant2.setChat(savedChat);
        chatParticipant2.setId(chatParticipantId2);
        chatParticipant2.setIsDeleted(false);
        chatParticipant2.setIsMuted(false);
        chatParticipant2.setIsChatEmpty(true);
        chatParticipant1.setUnreadCount(0);

        savedChat.addParticipant(chatParticipant1);
        savedChat.addParticipant(chatParticipant2);
        return chatRepository.save(savedChat);
    }

    protected Message createMessage(Long chatId, Long senderId, Long receiverId, String text) {
        Message newMessage = new Message();
        newMessage.setChatId(chatId);
        newMessage.setSenderId(senderId);
        newMessage.setReceiverId(receiverId);
        newMessage.setText(text);
        newMessage.setCreatedAt(OffsetDateTime.now());
        newMessage.setTimestamp(OffsetDateTime.now());
        return messageRepository.save(newMessage);
    }

    protected void clearUp() {
        messageRepository.deleteAll();
        chatRepository.deleteAll();
        userRepository.deleteAll();
    }
}
