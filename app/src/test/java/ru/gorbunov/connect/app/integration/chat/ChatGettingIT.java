package ru.gorbunov.connect.app.integration.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import ru.gorbunov.connect.app.integration.BaseIT;
import ru.gorbunov.connect.core.dto.user.UserCreateRequest;
import ru.gorbunov.connect.core.models.Chat;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.models.User;
import static org.assertj.core.api.Assertions.assertThat;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class ChatGettingIT extends BaseIT {
    private final String baseUrl = "/api/v1/chats";

    @Nested
    @DisplayName("Успешное получение списка чатов (Бизнес логика)")
    class SuccessScenarios {
        private User userA;
        private User userB;
        private User userC;

        private String jwtTokenA;
        private String jwtTokenB;
        private String jwtTokenC;

        private Chat chatAB;
        private Chat chatAC;

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

            chatAB = createChat(userA, userB);
            chatAC = createChat(userA, userC);

            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("Получение списка чатов для userA  - ожидается 200 ok")
        void getChatForUserA_validData_returns200() throws Exception {

            mockMvc.perform(get(baseUrl)
                            .header("Authorization", "Bearer " + jwtTokenA)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(hasSize(2)))
                    .andExpect(jsonPath("$[*].id", containsInAnyOrder(chatAB.getId().toString(), chatAC.getId().toString())))
                    // Фильтруем массив по ID чата, чтобы точечно и надежно проверить участников
                    .andExpect(jsonPath("$[?(@.id == '" + chatAB.getId() + "')].participants[*]",
                            containsInAnyOrder(userA.getId().toString(), userB.getId().toString())))
                    .andExpect(jsonPath("$[?(@.id == '" + chatAC.getId() + "')].participants[*]",
                            containsInAnyOrder(userA.getId().toString(), userC.getId().toString())));
        }

        @Test
        @DisplayName("Получение списка чатов для userB - ожидается 200 ok")
        void getChatForUserB_validData_returns200() throws Exception {

            mockMvc.perform(get(baseUrl)
                            .header("Authorization", "Bearer " + jwtTokenB)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(chatAB.getId().toString()))
                    .andExpect(jsonPath("$[0].participants", containsInAnyOrder(
                            String.valueOf(userA.getId()),
                            String.valueOf(userB.getId())
                    )));
        }

        @Test
        @DisplayName("Получение списка чатов для userC - ожидается 200 ok")
        void getChatForUserC_validData_returns200() throws Exception {

            mockMvc.perform(get(baseUrl)
                            .header("Authorization", "Bearer " + jwtTokenC)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(chatAC.getId().toString()))
                    .andExpect(jsonPath("$[0].participants", containsInAnyOrder(
                            String.valueOf(userA.getId()),
                            String.valueOf(userC.getId())
                    )));
        }

        @Test
        @DisplayName("Удаление чата AB для userA - ожидается ответ для userA(чат AC), для userB(чат AB)")
        void deleteChatForUserA_shouldRemoveChatOnlyForThatUser() throws Exception {
            chatService.deleteChatForUser(chatAB.getId(), userA.getId());

            mockMvc.perform(get(baseUrl)
                            .header("Authorization", "Bearer " + jwtTokenA)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(chatAC.getId().toString()));

            mockMvc.perform(get(baseUrl)
                            .header("Authorization", "Bearer " + jwtTokenB)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(chatAB.getId().toString()));
        }

        @Test
        @DisplayName("Удаление чата AB для userA и userB - ожидается полностью удаленный чат из базы данных")
        void deleteChatForUserAAndUserB_shouldRemoveChatForBothUsers() throws Exception {
            chatService.deleteChatForUser(chatAB.getId(), userA.getId());
            chatService.deleteChatForUser(chatAB.getId(), userB.getId());

            mockMvc.perform(get(baseUrl)
                            .header("Authorization", "Bearer " + jwtTokenA)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(chatAC.getId().toString()));

            mockMvc.perform(get(baseUrl)
                            .header("Authorization", "Bearer " + jwtTokenB)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(hasSize(0)));

            assertThat(chatRepository.findByDirectKey(chatAB.getDirectKey())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Проверка безопасности и авторизации (JWT)")
    class SecurityScenarios {
        private User tempUser;
        private String tempJwtToken;

        @BeforeEach
        void init() {
            var tempUserCreateRequest = new UserCreateRequest(
                    "tempuser",
                    "Firstname",
                    "Lastname",
                    "12345678",
                    "token"
            );
            tempUser = userService.create(tempUserCreateRequest, Role.ROLE_USER);
            tempJwtToken = jwtUtil.generateToken(tempUser);
        }

        @Test
        @DisplayName("Запрос без заголовка Authorization - ожидается 401")
        void getChat_withoutJwt_returns401() throws Exception {
            mockMvc.perform(get(baseUrl)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Запрос без заголовка Authorization - ожидается 401")
        void getChat_invalidJwt_returns401() throws Exception {
            mockMvc.perform(get(baseUrl)
                            .header("Authorization", "Bearer " + "invalidJwt")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Запрос удаленного пользователя - ожидается 410")
        void getChat_deletedUser_returns401() throws Exception {
            userDeletionService.softDelete(tempUser.getId());

            mockMvc.perform(get(baseUrl)
                            .header("Authorization", "Bearer " + tempJwtToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isGone());
        }

        @Test
        @DisplayName("Запрос заблокированного админом пользователя - ожидается 401")
        void getChat_blockedUser_returns401() throws Exception {
            banService.toggleUserBlockStatus(tempUser.getId(), true);

            mockMvc.perform(get(baseUrl)
                            .header("Authorization", "Bearer " + tempJwtToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }
}
