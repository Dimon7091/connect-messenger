package ru.connect.messenger.integration.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import ru.connect.messenger.integration.BaseIT;
import ru.connect.messenger.features.auth.AuthRequest;
import ru.connect.messenger.features.user.domain.Role;
import ru.connect.messenger.features.user.domain.User;
import ru.connect.messenger.features.user.dto.UserCreateRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class UserAuthenticationIT extends BaseIT {
    private final String url = "/api/v1/auth/login";
    private String userName1 = "user1";
    private String userPassword1 = "user_password1";

    private String userName2 = "user2";
    private String userPassword2 = "user_password2";

    private User testUser1;
    private User testUser2;

    @BeforeEach
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
        testUser1 = getUserServiceImpl().create(request1, Role.ROLE_USER);
        testUser2 = getUserServiceImpl().create(request2, Role.ROLE_USER);
    }

    @Test
    @DisplayName("Вход пользователя - ожидается 200 ok")
    void loginUser_validCredentials_returns200() throws Exception {
        var request = new AuthRequest(
                userName1,
                userPassword1
        );

        getMockMvc().perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getObjectMapper().writeValueAsString(request)))
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

        getMockMvc().perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getObjectMapper().writeValueAsString(request)))
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

        getMockMvc().perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getObjectMapper().writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Вход пользователя с паролем от другого аккаунта - ожидается 401 unauthorized")
    void loginUser_anotherPassword_returns401() throws Exception {
        var request = new AuthRequest(
                userName1,
                userPassword2
        );

        getMockMvc().perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getObjectMapper().writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Вход пользователя c несуществующим username - ожидается 401 unauthorized")
    void loginUser_nonExistentUser_returns401() throws Exception {
        var request = new AuthRequest(
                "non-existent-user",
                userPassword2
        );

        getMockMvc().perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getObjectMapper().writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Вход пользователя c пустым username - ожидается 422")
    void loginUser_emptyUserName_returns422() throws Exception {
        var request = new AuthRequest(
                "",
                userPassword2
        );

        getMockMvc().perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getObjectMapper().writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Вход заблокированного пользователя - ожидается 401")
    void loginUser_userBlocked_returns401() throws Exception {
        getUserBanService().toggleUserBlockStatus(testUser2.getId(), true);

        var request = new AuthRequest(
                userName2,
                userPassword2
        );

        getMockMvc().perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getObjectMapper().writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Вход удаленного пользователя - ожидается 401")
    void loginUser_userDeleted_returns401() throws Exception {
        getUserDeletionOrchestrator().softDelete(testUser2.getId());

        var request = new AuthRequest(
                userName2,
                userPassword2
        );

        getMockMvc().perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getObjectMapper().writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
