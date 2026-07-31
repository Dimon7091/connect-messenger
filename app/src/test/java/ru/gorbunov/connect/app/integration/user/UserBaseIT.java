package ru.gorbunov.connect.app.integration.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.gorbunov.connect.core.repository.UserRepository;
import ru.gorbunov.connect.core.service.InviteService;
import ru.gorbunov.connect.core.service.UserService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserBaseIT {
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected UserService userService;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected InviteService inviteService;

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
