package ru.connect.messenger.integration.message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import ru.connect.messenger.integration.BaseIT;
import ru.connect.messenger.features.messaging.chat.domain.Chat;
import ru.connect.messenger.features.messaging.message.domain.Message;
import ru.connect.messenger.features.user.domain.Role;
import ru.connect.messenger.features.user.domain.User;
import ru.connect.messenger.features.user.dto.UserCreateRequest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class MessageGettingIT extends BaseIT {
    private String baseUrl = "/api/v1/messages/chats";

    private User userA;
    private User userB;
    private User userC;

    private String jwtTokenA;
    private String jwtTokenB;
    private String jwtTokenC;

    private Chat chatAB;
    private Chat chatAC;
    private Chat chatCB;

    private Message chatMessageAB;
    private Message chatMessageAC;
    private Message chatMessageCB;

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
        userA = getUserServiceImpl().create(userACreateRequest, Role.ROLE_USER);
        userB = getUserServiceImpl().create(userBCreateRequest, Role.ROLE_USER);
        userC = getUserServiceImpl().create(userCCreateRequest, Role.ROLE_USER);
        jwtTokenA = getJwtTokenProvider().generateToken(userA);
        jwtTokenB = getJwtTokenProvider().generateToken(userB);
        jwtTokenC = getJwtTokenProvider().generateToken(userC);

        chatAB = createChat(userA, userB);
        chatAC = createChat(userA, userC);
        chatCB = createChat(userC, userB);

        chatMessageAB = createMessage(chatAB.getId(), userA.getId(), userB.getId(), "СообщениеAB");
        chatMessageAC = createMessage(chatAC.getId(), userA.getId(), userC.getId(), "СообщениеAC");
        chatMessageCB = createMessage(chatCB.getId(), userC.getId(), userB.getId(), "СообщениеCB");
    }

    @Test
    @DisplayName("Проверка получения сообщений пользователем userA")
    void getMessages_userA_returns200() throws Exception {

        getMockMvc().perform(get(baseUrl + "/" + chatAB.getId())
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + jwtTokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(chatMessageAB.getId()))
                .andExpect(jsonPath("$[0].chatId").value(chatAB.getId()))
                .andExpect(jsonPath("$[0].text").value(chatMessageAB.getText()));


        getMockMvc().perform(get(baseUrl + "/" + chatAC.getId())
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + jwtTokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(chatMessageAC.getId()))
                .andExpect(jsonPath("$[0].chatId").value(chatAC.getId()))
                .andExpect(jsonPath("$[0].text").value(chatMessageAC.getText()));

        // Проверка что пользователь не получл сообщения где он не является участником
        getMockMvc().perform(get(baseUrl + "/" + chatCB.getId())
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + jwtTokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(0)));
    }

    @Test
    @DisplayName("Проверка получения сообщений пользователем userB")
    void getMessages_userB_returns200() throws Exception {

        getMockMvc().perform(get(baseUrl + "/" + chatAB.getId())
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + jwtTokenB)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(chatMessageAB.getId()))
                .andExpect(jsonPath("$[0].chatId").value(chatAB.getId()))
                .andExpect(jsonPath("$[0].text").value(chatMessageAB.getText()));


        getMockMvc().perform(get(baseUrl + "/" + chatCB.getId())
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + jwtTokenB)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(chatMessageCB.getId()))
                .andExpect(jsonPath("$[0].chatId").value(chatCB.getId()))
                .andExpect(jsonPath("$[0].text").value(chatMessageCB.getText()));

        // Проверка что пользователь не получл сообщения где он не является участником
        getMockMvc().perform(get(baseUrl + "/" + chatAC.getId())
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + jwtTokenB)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(0)));
    }

    @Test
    @DisplayName("Проверка получения сообщений пользователем userC")
    void getMessages_userC_returns200() throws Exception {

        getMockMvc().perform(get(baseUrl + "/" + chatCB.getId())
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + jwtTokenC)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(chatMessageCB.getId()))
                .andExpect(jsonPath("$[0].chatId").value(chatCB.getId()))
                .andExpect(jsonPath("$[0].text").value(chatMessageCB.getText()));


        getMockMvc().perform(get(baseUrl + "/" + chatAC.getId())
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + jwtTokenC)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(chatMessageAC.getId()))
                .andExpect(jsonPath("$[0].chatId").value(chatAC.getId()))
                .andExpect(jsonPath("$[0].text").value(chatMessageAC.getText()));

        // Проверка что пользователь не получл сообщения где он не является участником
        getMockMvc().perform(get(baseUrl + "/" + chatAB.getId())
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + jwtTokenC)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(0)));
    }

}
