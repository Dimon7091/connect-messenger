package ru.gorbunov.connect.app.integration.message;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import ru.gorbunov.connect.app.integration.BaseIT;
import ru.gorbunov.connect.core.dto.user.UserCreateRequest;
import ru.gorbunov.connect.core.dto.ws.MessagesDeletedRequest;
import ru.gorbunov.connect.core.models.Chat;
import ru.gorbunov.connect.core.models.Message;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.models.User;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@Transactional
public class MessageDeletionIT extends BaseIT {
    private String baseUrl = "/api/v1/messages/batch";

    private User userA;
    private User userB;
    private User userC;

    private String jwtTokenA;
    private String jwtTokenB;
    private String jwtTokenC;

    private Chat chatAB;

    @BeforeEach
    void init() {
        var userACreateRequest = new UserCreateRequest(
                "user-a",
                "FirstName",
                "LastName",
                "12345678",
                "token"
        );
        var userBCreateRequest = new UserCreateRequest(
                "user-b",
                "FirstName",
                "LastName",
                "12345678",
                "token"
        );
        var userCCreateRequest = new UserCreateRequest(
                "user-c",
                "FirstName",
                "LastName",
                "12345678",
                "token"
        );

        userA = userService.create(userACreateRequest, Role.ROLE_USER);
        userB = userService.create(userBCreateRequest, Role.ROLE_USER);
        userC = userService.create(userCCreateRequest, Role.ROLE_USER);

        jwtTokenA = jwtUtil.generateToken(userA);
        jwtTokenB = jwtUtil.generateToken(userB);
        jwtTokenC = jwtUtil.generateToken(userC);

        chatAB = createChat(userA, userB);
    }

    @Test
    @DisplayName("Удаление собственного сообщения - ожидается 204")
    void deleteOwnMessage_userA_returns204() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.now().plusMinutes(1);

        var message = createMessage(
                chatAB.getId(),
                userA.getId(),
                userB.getId(),
                "Сообщение userA"
        );

        MessagesDeletedRequest request = new MessagesDeletedRequest(
                chatAB.getId(),
                List.of(message.getId())
        );

        mockMvc.perform(post(baseUrl)
                        .header("Authorization", "Bearer " + jwtTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Проверка базы данных при запросе пользователем A
        List<Message> userAMessages = messageRepository.findChatMessages(chatAB.getId(), 1, userA.getId(), timestamp);
        assertThat(userAMessages)
                .withFailMessage("При запросе пользователем A найдены сообщения в базе данных!")
                .isEmpty();
        // Проверка базы данных при запросе пользователем И
        List<Message> userBMessages = messageRepository.findChatMessages(chatAB.getId(), 1, userB.getId(), timestamp);
        assertThat(userBMessages)
                .withFailMessage("При запросе пользователем B найдены сообщения в базе данных!")
                .isEmpty();
    }

    @Test
    @DisplayName("Удаление сообщения собеседника - ожидается 204")
    void deleteCompanionMessage_userA_returns204() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.now().plusMinutes(1);

        var message = createMessage(
                chatAB.getId(),
                userB.getId(),
                userA.getId(),
                "Сообщение userB"
        );

        MessagesDeletedRequest request = new MessagesDeletedRequest(
                chatAB.getId(),
                List.of(message.getId())
        );

        mockMvc.perform(post(baseUrl)
                        .header("Authorization", "Bearer " + jwtTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Проверка базы данных при запросе пользователем A
        List<Message> userAMessages = messageRepository.findChatMessages(chatAB.getId(), 1, userA.getId(), timestamp);
        assertThat(userAMessages)
                .withFailMessage("При запросе пользователем A найдены сообщения в базе данных!")
                .isEmpty();
        // Проверка базы данных при запросе пользователем И
        List<Message> userBMessages = messageRepository.findChatMessages(chatAB.getId(), 1, userB.getId(), timestamp);
        assertThat(userBMessages)
                .withFailMessage("При запросе пользователем B найдены сообщения в базе данных!")
                .isEmpty();
    }

    @Test
    @DisplayName("Удаление сообщения пользователем который не является участником - ожидается 403")
    void deleteForeignMessage_userC_returns403() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.now().plusMinutes(1);

        var message = createMessage(
                chatAB.getId(),
                userA.getId(),
                userB.getId(),
                "Сообщение userA"
        );

        MessagesDeletedRequest request = new MessagesDeletedRequest(
                chatAB.getId(),
                List.of(message.getId())
        );

        mockMvc.perform(post(baseUrl)
                        .header("Authorization", "Bearer " + jwtTokenC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Проверка базы данных при запросе пользователем A
        List<Message> userAMessages = messageRepository.findChatMessages(chatAB.getId(), 1, userA.getId(), timestamp);
        assertThat(userAMessages)
                .withFailMessage("Нет сообщений при запросе пользователем A!")
                .isNotEmpty()
                .singleElement()
                .extracting(ru.gorbunov.connect.core.models.Message::getText)
                .isEqualTo(message.getText());

        // Проверка базы данных при запросе пользователем B
        List<Message> userBMessages = messageRepository.findChatMessages(chatAB.getId(), 1, userB.getId(), timestamp);
        assertThat(userBMessages)
                .withFailMessage("Нет сообщений при запросе пользователем B!")
                .isNotEmpty()
                .singleElement()
                .extracting(ru.gorbunov.connect.core.models.Message::getText)
                .isEqualTo(message.getText());
    }

    @Test
    @DisplayName("Удаления истории сообщений при удалении чата обоями участниками")
    void deleteChat_byBothParticipant_deletedChatAndMessages() throws Exception {
        String deleteChatUrl = "/api/v1/chats";

        OffsetDateTime timestamp = OffsetDateTime.now().plusMinutes(1);

        var userAmessage = createMessage(
                chatAB.getId(),
                userA.getId(),
                userB.getId(),
                "Сообщение userA"
        );

        var userBmessage = createMessage(
                chatAB.getId(),
                userA.getId(),
                userB.getId(),
                "Сообщение userA"
        );

        // Зарос на удаление чата пользователя A
        mockMvc.perform(delete(deleteChatUrl + "/" + chatAB.getId())
                        .header("Authorization", "Bearer " + jwtTokenA))
                .andExpect(status().isNoContent());

        // Зарос на удаление чата пользователя B
        mockMvc.perform(delete(deleteChatUrl + "/" + chatAB.getId())
                        .header("Authorization", "Bearer " + jwtTokenB))
                .andExpect(status().isNoContent());

        // Проверка базы данных
        assertThat(messageRepository.existsById(userAmessage.getId()))
                .withFailMessage("Сообщение пользователя A не удалилось из базы данных!")
                .isFalse();

        assertThat(messageRepository.existsById(userBmessage.getId()))
                .withFailMessage("Сообщение пользователя B не удалилось из базы данных!")
                .isFalse();

        assertThat(chatRepository.existsById(chatAB.getId()))
                .withFailMessage("Чат не удалился из базы данных!")
                .isFalse();
    }
}
