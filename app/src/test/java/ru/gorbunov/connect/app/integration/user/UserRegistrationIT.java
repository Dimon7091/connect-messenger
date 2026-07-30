package ru.gorbunov.connect.app.integration.user;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

    @Test
    @DisplayName("Создание пользователя: возвращает 200 OK при валидных данных")
    void createUser_validData_returns200() throws Exception {
        String invitationToken = createInvitationToken();
        var request = new UserCreateRequest(
                "user1",
                "djon",
                "staper",
                "12345678",
                invitationToken
        );

        mockMvc.perform(post("/api/labels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userName").value(request.userName()))
                .andExpect(jsonPath("$.firstName").value(request.firstName()))
                .andExpect(jsonPath("$.lastName").value(request.lastName()));
    }

    String createInvitationToken() throws MalformedURLException {
        String invitationToken = "";

        URL uri;
        uri = new URL(inviteService.create().invitationUrl());
        String query = uri.getQuery();
        invitationToken = Arrays.stream(query.split("&"))
                .map(param -> param.split("="))
                .filter(pair -> pair.length > 1 && pair[0].equals("invitationToken"))
                .map(pair -> pair[1])
                .findFirst()
                .orElse(null);
        return invitationToken;
    }

}
