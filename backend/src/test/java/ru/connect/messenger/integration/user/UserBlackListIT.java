package ru.connect.messenger.integration.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import ru.connect.messenger.integration.BaseIT;
import ru.connect.messenger.features.user.domain.Role;
import ru.connect.messenger.features.user.domain.User;
import ru.connect.messenger.features.user.dto.UserCreateRequest;

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
        userA = getUserService().create(userACreateRequest, Role.ROLE_USER);
        userB = getUserService().create(userBCreateRequest, Role.ROLE_USER);
        userC = getUserService().create(userCCreateRequest, Role.ROLE_USER);
        jwtTokenA = getJwtTokenProvider().generateToken(userA);
        jwtTokenB = getJwtTokenProvider().generateToken(userB);
        jwtTokenC = getJwtTokenProvider().generateToken(userC);
    }

    @Test
    @DisplayName("UserA заносит id в черный список userB, userC - ожидется ответ с добавленными пользователями")
    void addToBlackList_userBAndUserCBlockedByUserA_returnUserDtoWithBlockedUsers() throws Exception {
        // Провека занесения id в черный список
        getMockMvc().perform(post(baseUrl + "/" + userB.getId())
                        .header("Authorization", "Bearer " + jwtTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockedUserId").value(userB.getId()));

        getMockMvc().perform(post(baseUrl + "/" + userC.getId())
                        .header("Authorization", "Bearer " + jwtTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockedUserId").value(userC.getId()));

        // Проверка получения списка всех заблокированных пользователей
        getMockMvc().perform(get(baseUrl)
                        .header("Authorization", "Bearer " + jwtTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id",
                        containsInAnyOrder(userB.getId().toString(), userC.getId().toString())));
    }

    @Test
    @DisplayName("UserA удаляет всех из черного списка - ожидется ответ 204")
    void removeFromBlackList_userARemoveUserBAndUserCFromBlacklist_returns204() throws Exception {
        getUserBlockService().blockUserByUser(userA.getId(), userB.getId());
        getUserBlockService().blockUserByUser(userA.getId(), userC.getId());

        getMockMvc().perform(delete(baseUrl + "/" + userB.getId())
                        .header("Authorization", "Bearer " + jwtTokenA))
                .andExpect(status().isNoContent());

        getMockMvc().perform(delete(baseUrl + "/" + userC.getId())
                        .header("Authorization", "Bearer " + jwtTokenA))
                .andExpect(status().isNoContent());

        // Проверка получения списка всех заблокированных пользователей
        getMockMvc().perform(get(baseUrl)
                        .header("Authorization", "Bearer " + jwtTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
