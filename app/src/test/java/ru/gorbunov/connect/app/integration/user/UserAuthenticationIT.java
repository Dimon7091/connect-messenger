package ru.gorbunov.connect.app.integration.user;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.MediaType;
import ru.gorbunov.connect.app.integration.BaseIT;
import ru.gorbunov.connect.core.dto.user.UserCreateRequest;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.models.User;
import ru.gorbunov.connect.web.dto.AuthRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserAuthenticationIT extends BaseIT {
    private final String url = "/api/v1/auth/login";
    private String userName1 = "user1";
    private String userPassword1 = "user_password1";

    private String userName2 = "user2";
    private String userPassword2 = "user_password2";

    private User testUser1;
    private User testUser2;

    @BeforeAll
    void registerUser() {
        var request1 = new UserCreateRequest(
                userName1,
                "FirstName",
                "LastName",
                userPassword1,
                "token"
        );
        var request2 = new UserCreateRequest(
                userName2,
                "FirstName",
                "LastName",
                userPassword2,
                "token"
        );
        testUser1 = userService.create(request1, Role.ROLE_USER);
        testUser2 = userService.create(request2, Role.ROLE_USER);
    }

    @Test
    @DisplayName("Вход пользователя - ожидается 200 ok")
    void loginUser_validCredentials_returns200() throws Exception {
        var request = new AuthRequest(
                userName1,
                userPassword1
        );

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.userName").value(userName1))
                .andExpect(jsonPath("$.token").exists());

    }

    @Test
    @DisplayName("Вход пользователя, верхний регистр - ожидается 200 ok")
    void loginUser_uppercaseUserName_returns200() throws Exception {
        var request = new AuthRequest(
                "User1",
                userPassword1
        );

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.userName").value(userName1))
                .andExpect(jsonPath("$.token").exists());

    }

    @Test
    @DisplayName("Вход пользователя с невалидным паролем - ожидается 401 unauthorized")
    void loginUser_invalidPassword_returns401() throws Exception {
        var request = new AuthRequest(
                userName1,
                "incorrect_password"
        );

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Вход пользователя с паролем от другого аккаунта - ожидается 401 unauthorized")
    void loginUser_anotherPassword_returns401() throws Exception {
        var request = new AuthRequest(
                userName1,
                userPassword2
        );

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Вход пользователя c несуществующим username - ожидается 401 unauthorized")
    void loginUser_nonExistentUser_returns401() throws Exception {
        var request = new AuthRequest(
                "non-existent-user",
                userPassword2
        );

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Вход пользователя c пустым username - ожидается 422")
    void loginUser_emptyUserName_returns422() throws Exception {
        var request = new AuthRequest(
                "",
                userPassword2
        );

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Вход заблокированного пользователя - ожидается 401")
    void loginUser_userBlocked_returns401() throws Exception {
        banService.toggleUserBlockStatus(testUser2.getId(), true);

        var request = new AuthRequest(
                userName2,
                userPassword2
        );

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Вход удаленного пользователя - ожидается 401")
    void loginUser_userDeleted_returns401() throws Exception {
        userDeletionService.softDelete(testUser2.getId());

        var request = new AuthRequest(
                userName2,
                userPassword2
        );

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @AfterAll
    void cleanUpAll() {
        userRepository.deleteAllInBatch();
    }
}
