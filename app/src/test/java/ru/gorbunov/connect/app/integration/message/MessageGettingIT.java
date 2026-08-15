package ru.gorbunov.connect.app.integration.message;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import ru.gorbunov.connect.app.integration.BaseIT;
import ru.gorbunov.connect.core.dto.user.UserCreateRequest;
import ru.gorbunov.connect.core.models.Chat;
import ru.gorbunov.connect.core.models.Message;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.models.User;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
        userA = userService.create(userACreateRequest, Role.ROLE_USER);
        userB = userService.create(userBCreateRequest, Role.ROLE_USER);
        userC = userService.create(userCCreateRequest, Role.ROLE_USER);
        jwtTokenA = jwtUtil.generateToken(userA);
        jwtTokenB = jwtUtil.generateToken(userB);
        jwtTokenC = jwtUtil.generateToken(userC);

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

        mockMvc.perform(get(baseUrl + "/" + chatAB.getId())
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + jwtTokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(chatMessageAB.getId()))
                .andExpect(jsonPath("$[0].chatId").value(chatAB.getId()))
                .andExpect(jsonPath("$[0].text").value(chatMessageAB.getText()));


        mockMvc.perform(get(baseUrl + "/" + chatAC.getId())
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + jwtTokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(chatMessageAC.getId()))
                .andExpect(jsonPath("$[0].chatId").value(chatAC.getId()))
                .andExpect(jsonPath("$[0].text").value(chatMessageAC.getText()));

        // Проверка что пользователь не получл сообщения где он не является участником
        mockMvc.perform(get(baseUrl + "/" + chatCB.getId())
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + jwtTokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(0)));
    }

    @Test
    @DisplayName("Проверка получения сообщений пользователем userB")
    void getMessages_userB_returns200() throws Exception {

        mockMvc.perform(get(baseUrl + "/" + chatAB.getId())
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + jwtTokenB)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(chatMessageAB.getId()))
                .andExpect(jsonPath("$[0].chatId").value(chatAB.getId()))
                .andExpect(jsonPath("$[0].text").value(chatMessageAB.getText()));


        mockMvc.perform(get(baseUrl + "/" + chatCB.getId())
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + jwtTokenB)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(chatMessageCB.getId()))
                .andExpect(jsonPath("$[0].chatId").value(chatCB.getId()))
                .andExpect(jsonPath("$[0].text").value(chatMessageCB.getText()));

        // Проверка что пользователь не получл сообщения где он не является участником
        mockMvc.perform(get(baseUrl + "/" + chatAC.getId())
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + jwtTokenB)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(0)));
    }

    @Test
    @DisplayName("Проверка получения сообщений пользователем userC")
    void getMessages_userC_returns200() throws Exception {

        mockMvc.perform(get(baseUrl + "/" + chatCB.getId())
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + jwtTokenC)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(chatMessageCB.getId()))
                .andExpect(jsonPath("$[0].chatId").value(chatCB.getId()))
                .andExpect(jsonPath("$[0].text").value(chatMessageCB.getText()));


        mockMvc.perform(get(baseUrl + "/" + chatAC.getId())
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + jwtTokenC)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(chatMessageAC.getId()))
                .andExpect(jsonPath("$[0].chatId").value(chatAC.getId()))
                .andExpect(jsonPath("$[0].text").value(chatMessageAC.getText()));

        // Проверка что пользователь не получл сообщения где он не является участником
        mockMvc.perform(get(baseUrl + "/" + chatAB.getId())
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + jwtTokenC)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(0)));
    }

    @AfterAll
    @Transactional
    void clearUp() {
        messageRepository.deleteAll();
        chatRepository.deleteAll();
        userRepository.deleteAll();
    }
}
