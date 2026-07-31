package ru.gorbunov.connect.app.integration.user;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import ru.gorbunov.connect.core.dto.user.UserCreateRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public class UserRegistrationIT extends UserBaseIT{
    private final String url = "/api/v1/auth/register";

    @Nested
    @DisplayName("Создание пользователя — валидные данные")
    class UserCreateValid {

        @ParameterizedTest(name = "[{index}] Юзернейм: \"{0}\" -> Ожидается 201 Created")
        @ValueSource(strings = {"use", "user", "user1", "user_1", "user-1", "user."})
        void createUser_validUserName_returns201(String validUserName) throws Exception {
            String invitationToken = createInvitationToken();
            var request = new UserCreateRequest(
                    validUserName,
                    "Name",
                    "LastName",
                    "12345678",
                    invitationToken
            );

            mockMvc.perform(post(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.user.userName").value(validUserName))
                    .andExpect(jsonPath("$.user.profile.firstName").value(request.firstName()))
                    .andExpect(jsonPath("$.user.profile.lastName").value(request.lastName()));
        }

        @ParameterizedTest(name = "[{index}] Имя: \"{0}\" -> Ожидается 201 Created")
        @ValueSource(strings = {"Имя", "Name", "имя"})
        void createUser_validFistName_returns201(String validFirsName) throws Exception {
            String invitationToken = createInvitationToken();

            var request = new UserCreateRequest(
                    "user",
                    validFirsName,
                    "LastName",
                    "12345678",
                    invitationToken
            );

            String upperCaseFirstName = request.firstName().substring(0, 1).toUpperCase() + request.firstName().substring(1);

            mockMvc.perform(post(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.user.userName").value(request.userName()))
                    .andExpect(jsonPath("$.user.profile.firstName").value(upperCaseFirstName))
                    .andExpect(jsonPath("$.user.profile.lastName").value(request.lastName()));
        }

        @ParameterizedTest(name = "[{index}] Фамилия: \"{0}\" -> Ожидается 201 Created")
        @ValueSource(strings = {"Фамилия", "LastName", "фамилия"})
        void createUser_validLastName_returns201(String validLastName) throws Exception {
            String invitationToken = createInvitationToken();

            var request = new UserCreateRequest(
                    "user",
                    "FirstName",
                    validLastName,
                    "12345678",
                    invitationToken
            );

            String upperCaseLastName = request.lastName().substring(0, 1).toUpperCase() + request.lastName().substring(1);

            mockMvc.perform(post(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.user.userName").value(request.userName()))
                    .andExpect(jsonPath("$.user.profile.firstName").value(request.firstName()))
                    .andExpect(jsonPath("$.user.profile.lastName").value(upperCaseLastName));
        }

        @ParameterizedTest(name = "[{index}] Юзернейм: \"{0}\" -> Ожидается 201 Created")
        @ValueSource(strings = {"12345678-asdfasdf", "asdfas1?$%", "12345678"})
        void createUser_validPassword_returns201(String validPassword) throws Exception {
            String invitationToken = createInvitationToken();
            var request = new UserCreateRequest(
                    "user",
                    "Name",
                    "LastName",
                    validPassword,
                    invitationToken
            );

            mockMvc.perform(post(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.user.userName").value(request.userName()))
                    .andExpect(jsonPath("$.user.profile.firstName").value(request.firstName()))
                    .andExpect(jsonPath("$.user.profile.lastName").value(request.lastName()));
        }
    }

    @Nested
    @DisplayName("Создание пользователя — не валидные данные")
    class UserCreateInvalid {

        @ParameterizedTest(name = "[{index}] Юзернейм: \"{0}\" -> Ожидается 422 unprocessable")
        @ValueSource(strings = {"us", "user*", "user/", "user~", "user#", "user&", "user?", "user!",
                "...", "///", "&&&", "", "111", "over-twenty-simbol-username"})
        void createUser_invalidUserName_returns422(String invalidUserName) throws Exception {
            String invitationToken = createInvitationToken();
            var request = new UserCreateRequest(
                    invalidUserName,
                    "Name",
                    "LastName",
                    "12345678",
                    invitationToken
            );

            mockMvc.perform(post(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @ParameterizedTest(name = "[{index}] Имя: \"{0}\" -> Ожидается 422 unprocessable")
        @ValueSource(strings = {"N", "over-fifty-simbol-name-aaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "Name1",
                "Name!", "!!!", "111", "???", "***", "...", ""})
        void createUser_invalidFirstName_returns422(String invalidFirstName) throws Exception {
            String invitationToken = createInvitationToken();
            var request = new UserCreateRequest(
                    "user",
                    invalidFirstName,
                    "LastName",
                    "12345678",
                    invitationToken
            );

            mockMvc.perform(post(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @ParameterizedTest(name = "[{index}] Фамилия: \"{0}\" -> Ожидается 422 unprocessable")
        @ValueSource(strings = {"N", "over-fifty-simbols-name-aaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "Name1",
                "Name!", "!!!", "111", "???", "***", "...", " "})
        void createUser_invalidLastName_returns422(String invalidLastName) throws Exception {
            String invitationToken = createInvitationToken();
            var request = new UserCreateRequest(
                    "user",
                    "FirstName",
                    invalidLastName,
                    "12345678",
                    invitationToken
            );

            mockMvc.perform(post(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @ParameterizedTest(name = "[{index}] Пароль: \"{0}\" -> Ожидается 422 unprocessable")
        @ValueSource(strings = {"1234567", "       ", "over-fifty-simbols-password-1111111111111111111111111"})
        void createUser_invalidPassword_returns422(String invalidPassword) throws Exception {
            String invitationToken = createInvitationToken();
            var request = new UserCreateRequest(
                    "user",
                    "FirstName",
                    "LasName",
                    invalidPassword,
                    invitationToken
            );

            mockMvc.perform(post(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("Не валидный токен: -> Ожидается 403 forbidde")
        void createUser_invalidToken_returns403() throws Exception {
            String invalidToken = createInvitationToken() + "a";
            var request = new UserCreateRequest(
                    "user",
                    "FirstName",
                    "LasName",
                    "12345678",
                    invalidToken
            );

            mockMvc.perform(post(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // Вспомогательный метод для создания приглашения
    String createInvitationToken() throws MalformedURLException {
        URL uri;
        uri = new URL(inviteService.create().invitationUrl());
        String query = uri.getQuery();
        return Arrays.stream(query.split("&"))
                .map(param -> param.split("="))
                .filter(pair -> pair.length > 1 && pair[0].equals("invitationToken"))
                .map(pair -> pair[1])
                .findFirst()
                .orElse(null);
    }
}
