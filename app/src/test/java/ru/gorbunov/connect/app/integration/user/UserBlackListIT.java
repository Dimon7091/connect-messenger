package ru.gorbunov.connect.app.integration.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import ru.gorbunov.connect.app.integration.BaseIT;
import ru.gorbunov.connect.core.dto.user.UserCreateRequest;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.models.User;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class UserBlackListIT extends BaseIT {
    private final String baseUrl = "/api/v1/users/me/blacklist";
    private User userA;
    private User userB;
    private User userC;

    private String jwtTokenA;
    private String jwtTokenB;
    private String jwtTokenC;

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
    }

    @Test
    @DisplayName("UserA заносит id в черный список userB, userC - ожидется ответ с добавленными пользователями")
    void addToBlackList_userBAndUserCBlockedByUserA_returnUserDtoWithBlockedUsers() throws Exception {
        // Провека занесения id в черный список
        mockMvc.perform(post(baseUrl + "/" + userB.getId())
                        .header("Authorization", "Bearer " + jwtTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockedUserId").value(userB.getId()));

        mockMvc.perform(post(baseUrl + "/" + userC.getId())
                        .header("Authorization", "Bearer " + jwtTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockedUserId").value(userC.getId()));

        // Проверка получения списка всех заблокированных пользователей
        mockMvc.perform(get(baseUrl)
                        .header("Authorization", "Bearer " + jwtTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id",
                        containsInAnyOrder(userB.getId().toString(), userC.getId().toString())));
    }

    @Test
    @DisplayName("UserA удаляет всех из черного списка - ожидется ответ 204")
    void removeFromBlackList_userARemoveUserBAndUserCFromBlacklist_returns204() throws Exception {
        userBlockService.blockUserByUser(userA.getId(), userB.getId());
        userBlockService.blockUserByUser(userA.getId(), userC.getId());

        mockMvc.perform(delete(baseUrl + "/" + userB.getId())
                        .header("Authorization", "Bearer " + jwtTokenA))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete(baseUrl + "/" + userC.getId())
                        .header("Authorization", "Bearer " + jwtTokenA))
                .andExpect(status().isNoContent());

        // Проверка получения списка всех заблокированных пользователей
        mockMvc.perform(get(baseUrl)
                        .header("Authorization", "Bearer " + jwtTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
