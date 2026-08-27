package ru.connect.messenger.integration.message;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import ru.connect.messenger.integration.BaseIT;
import ru.connect.messenger.features.messaging.chat.domain.Chat;
import ru.connect.messenger.features.messaging.message.domain.Message;
import ru.connect.messenger.features.messaging.message.dto.MessagesDeletedRequest;
import ru.connect.messenger.features.user.domain.Role;
import ru.connect.messenger.features.user.domain.User;
import ru.connect.messenger.features.user.dto.UserCreateRequest;

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
        clearUp();
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

        userA = getUserService().create(userACreateRequest, Role.ROLE_USER);
        userB = getUserService().create(userBCreateRequest, Role.ROLE_USER);
        userC = getUserService().create(userCCreateRequest, Role.ROLE_USER);

        jwtTokenA = getJwtTokenProvider().generateToken(userA);
        jwtTokenB = getJwtTokenProvider().generateToken(userB);
        jwtTokenC = getJwtTokenProvider().generateToken(userC);

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

        getMockMvc().perform(post(baseUrl)
                        .header("Authorization", "Bearer " + jwtTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getObjectMapper().writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Проверка базы данных при запросе пользователем A
        List<Message> userAMessages = getMessageRepository().findChatMessages(
                chatAB.getId(), 1, userA.getId(), timestamp
        );
        assertThat(userAMessages)
                .withFailMessage("При запросе пользователем A найдены сообщения в базе данных!")
                .isEmpty();
        // Проверка базы данных при запросе пользователем И
        List<Message> userBMessages = getMessageRepository().findChatMessages(
                chatAB.getId(), 1, userB.getId(), timestamp
        );
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

        getMockMvc().perform(post(baseUrl)
                        .header("Authorization", "Bearer " + jwtTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getObjectMapper().writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Проверка базы данных при запросе пользователем A
        List<Message> userAMessages = getMessageRepository().findChatMessages(
                chatAB.getId(), 1, userA.getId(), timestamp
        );
        assertThat(userAMessages)
                .withFailMessage("При запросе пользователем A найдены сообщения в базе данных!")
                .isEmpty();
        // Проверка базы данных при запросе пользователем И
        List<Message> userBMessages = getMessageRepository().findChatMessages(
                chatAB.getId(), 1, userB.getId(), timestamp
        );
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

        getMockMvc().perform(post(baseUrl)
                        .header("Authorization", "Bearer " + jwtTokenC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Проверка базы данных при запросе пользователем A
        List<Message> userAMessages = getMessageRepository().findChatMessages(
                chatAB.getId(), 1, userA.getId(), timestamp
        );
        assertThat(userAMessages)
                .withFailMessage("Нет сообщений при запросе пользователем A!")
                .isNotEmpty()
                .singleElement()
                .extracting(Message::getText)
                .isEqualTo(message.getText());

        // Проверка базы данных при запросе пользователем B
        List<Message> userBMessages = getMessageRepository().findChatMessages(
                chatAB.getId(), 1, userB.getId(), timestamp
        );
        assertThat(userBMessages)
                .withFailMessage("Нет сообщений при запросе пользователем B!")
                .isNotEmpty()
                .singleElement()
                .extracting(Message::getText)
                .isEqualTo(message.getText());
    }

    @Test
    @DisplayName("Удаления истории сообщений при удалении чата обоями участниками")
    void deleteChat_byBothParticipant_deletedChatAndMessages() throws Exception {
        String deleteChatUrl = "/api/v1/chats";

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
        getMockMvc().perform(delete(deleteChatUrl + "/" + chatAB.getId())
                        .header("Authorization", "Bearer " + jwtTokenA))
                .andExpect(status().isNoContent());

        // Зарос на удаление чата пользователя B
        getMockMvc().perform(delete(deleteChatUrl + "/" + chatAB.getId())
                        .header("Authorization", "Bearer " + jwtTokenB))
                .andExpect(status().isNoContent());

        // Проверка базы данных
        assertThat(getMessageRepository().existsById(userAmessage.getId()))
                .withFailMessage("Сообщение пользователя A не удалилось из базы данных!")
                .isFalse();

        assertThat(getMessageRepository().existsById(userBmessage.getId()))
                .withFailMessage("Сообщение пользователя B не удалилось из базы данных!")
                .isFalse();

        assertThat(getMessageRepository().existsById(chatAB.getId()))
                .withFailMessage("Чат не удалился из базы данных!")
                .isFalse();
    }
}
