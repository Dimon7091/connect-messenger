package ru.gorbunov.connect.app.integration.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.gorbunov.connect.core.service.InviteService;
import ru.gorbunov.connect.core.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RequiredArgsConstructor
public class UserBaseIT {
    protected MockMvc mockMvc;
    protected ObjectMapper objectMapper;
    protected UserService userService;
    protected InviteService inviteService;
}
