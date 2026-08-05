package ru.gorbunov.connect.app.integration.user;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import ru.gorbunov.connect.core.dto.user.UserCreateRequest;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.models.User;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserUpdatingIT extends UserBaseIT {
    private final String baseUrl = "/api/v1/users/me";

    private String jwtToken1;
    private String jwtToken2;
    private User testUser1;
    private User testUser2;

    @BeforeAll
    void testUsersInit() {
        var request1 = new UserCreateRequest(
                "username1",
                "Firstname",
                "Lastname",
                "userPassword1",
                "token"
        );
        var request2 = new UserCreateRequest(
                "username2",
                "Firstname",
                "Lastname",
                "userPassword2",
                "token"
        );
        testUser1 = userService.create(request1, Role.ROLE_USER);
        testUser2 = userService.create(request2, Role.ROLE_USER);

        jwtToken1 = jwtUtil.generateToken(testUser1);
        jwtToken2 = jwtUtil.generateToken(testUser2);
    }

    @Nested
    @DisplayName("Успешное обновление пользователя")
    @Transactional
    class UserUpdateSuccess {

        @ParameterizedTest(name = "[{index}] Юзернейм: \"{0}\" -> Ожидается 200 ok")
        @ValueSource(strings = {"use", "user", "user1", "user_1", "user-1", "user."})
        void updateUserName_validUserName_returns200(String validUserName) throws Exception {
            var request = Map.of(
                    "username", validUserName
            );

            mockMvc.perform(patch(baseUrl + "/update-username")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwtToken1)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testUser1.getId()))
                    .andExpect(jsonPath("$.userName").value(validUserName));
        }

        @ParameterizedTest(name = "[{index}] Имя: \"{0}\" -> Ожидается 200 ok")
        @ValueSource(strings = {"Имя", "Name", "имя"})
        void updateFirstName_valiFirstName_returns200(String validFirstName) throws Exception {
            var request = Map.of(
                    "firstName", validFirstName
            );

            mockMvc.perform(patch(baseUrl + "/profile/update")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwtToken1)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testUser1.getId()))
                    .andExpect(jsonPath("$.profile.firstName").value(validFirstName));
        }

        @ParameterizedTest(name = "[{index}] Фамилия: \"{0}\" -> Ожидается 200 ok")
        @ValueSource(strings = {"Фамилия", "LastName", "фамилия"})
        void updateLastName_valiLastName_returns200(String validLastName) throws Exception {
            var request = Map.of(
                    "lastName", validLastName
            );

            mockMvc.perform(patch(baseUrl + "/profile/update")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwtToken1)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testUser1.getId()))
                    .andExpect(jsonPath("$.profile.lastName").value(validLastName));
        }
    }

    @Nested
    @DisplayName("Не успешное обновление пользователя")
    @Transactional
    class UserUpdateUnsuccess {

        @ParameterizedTest(name = "[{index}] Юзернейм: \"{0}\" -> Ожидается 422")
        @ValueSource(strings = {"us", "user*", "user/", "user~", "user#", "user&", "user?", "user!",
                "...", "///", "&&&", "", "111", "over-twenty-simbol-username"})
        void updateUserName_invalidUserName_returns422(String invalidUserName) throws Exception {
            var request = Map.of(
                    "username", invalidUserName
            );

            mockMvc.perform(patch(baseUrl + "/update-username")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwtToken1)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @ParameterizedTest(name = "[{index}] Имя: \"{0}\" -> Ожидается 422")
        @ValueSource(strings = {"N", "over-fifty-simbol-name-aaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "Name1",
                "Name!", "!!!", "111", "???", "***", "...", ""})
        void updateFirstName_invalidFirstName_returns422(String invalidFirstName) throws Exception {
            var request = Map.of(
                    "firstName", invalidFirstName
            );

            mockMvc.perform(patch(baseUrl + "/profile/update")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwtToken1)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @ParameterizedTest(name = "[{index}] Фамилия: \"{0}\" -> Ожидается 422")
        @ValueSource(strings = {"N", "over-fifty-simbols-name-aaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "Name1",
                "Name!", "!!!", "111", "???", "***", "...", " "})
        void updateLastName_invalidLastName_returns422(String invalidLastName) throws Exception {
            var request = Map.of(
                    "lastName", invalidLastName
            );

            mockMvc.perform(patch(baseUrl + "/profile/update")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwtToken1)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
