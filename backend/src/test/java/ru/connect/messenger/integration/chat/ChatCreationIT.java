package ru.connect.messenger.integration.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import ru.connect.messenger.integration.BaseIT;
import ru.connect.messenger.features.messaging.chat.dto.ChatResponse;
import ru.connect.messenger.features.user.domain.Role;
import ru.connect.messenger.features.user.domain.User;
import ru.connect.messenger.features.user.dto.UserCreateRequest;

import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class ChatCreationIT extends BaseIT {
    private final String baseUrl = "/api/v1/chats";
    private User testUser1;
    private User testUser2;

    private String jwtToken1;
    private String jwtToken2;

    @BeforeEach
    void init() {
        clearUp();
        var request1 = new UserCreateRequest(
                "user1",
                "FirstName",
                "LastName",
                "12345678",
                "token"
        );
        var request2 = new UserCreateRequest(
                "user2",
                "FirstName",
                "LastName",
                "12345678",
                "token"
        );
        testUser1 = getUserServiceImpl().create(request1, Role.ROLE_USER);
        testUser2 = getUserServiceImpl().create(request2, Role.ROLE_USER);
        jwtToken1 = getJwtTokenProvider().generateToken(testUser1);
        jwtToken2 = getJwtTokenProvider().generateToken(testUser2);
    }

    @Test
    @DisplayName("Создание чата - ожидается 200 ok")
    void createChat_validData_returns200() throws Exception {
        var request = Map.of(
                "companionId", testUser2.getId()
        );

        getMockMvc().perform(post(baseUrl)
                        .header("Authorization", "Bearer " + jwtToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants", containsInAnyOrder(
                        String.valueOf(testUser1.getId()),
                        String.valueOf(testUser2.getId())
                )));
    }

    @Test
    @DisplayName("Создание существующего чата между пользователями - ожидается возврат существующего чата 200 ok")
    void createExistingChat_validData_returns200() throws Exception {
        var requestByUser1 = Map.of(
                "companionId", testUser2.getId()
        );

        var mvcResult = getMockMvc().perform(post(baseUrl)
                        .header("Authorization", "Bearer " + jwtToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getObjectMapper().writeValueAsString(requestByUser1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants", containsInAnyOrder(
                        String.valueOf(testUser1.getId()),
                        String.valueOf(testUser2.getId())
                )))
                .andReturn();

        String jsonResponse = mvcResult.getResponse().getContentAsString();
        ChatResponse existingChat = getObjectMapper().readValue(jsonResponse, ChatResponse.class);

        var requestByUser2 = Map.of(
                "companionId", testUser1.getId()
        );

        getMockMvc().perform(post(baseUrl)
                        .header("Authorization", "Bearer " + jwtToken2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getObjectMapper().writeValueAsString(requestByUser2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingChat.getId()))
                .andExpect(jsonPath("$.participants", containsInAnyOrder(
                        String.valueOf(testUser1.getId()),
                        String.valueOf(testUser2.getId())
                )))
                .andReturn();
    }

    @Test
    @DisplayName("Создание чата без авторизации - ожидается 401")
    void createChat_invalidData_returns401() throws Exception {
        var request = Map.of(
                "companionId", testUser2.getId()
        );

        getMockMvc().perform(post(baseUrl)
                        .header("Authorization", "Bearer " + "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getObjectMapper().writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Создание чата, не валидные данные - ожидается 400")
    void createChat_invalidData1_returns400() throws Exception {
        var request = Map.of(
                "companionId", "invalid-data"
        );

        getMockMvc().perform(post(baseUrl)
                        .header("Authorization", "Bearer " + jwtToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
