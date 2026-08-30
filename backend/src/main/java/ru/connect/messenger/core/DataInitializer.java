package ru.connect.messenger.core;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ru.connect.messenger.features.messaging.api.ChatService;
import ru.connect.messenger.features.messaging.message.api.MessageService;
import ru.connect.messenger.features.messaging.message.dto.SendMessageRequest;
import ru.connect.messenger.features.user.api.UserService;
import ru.connect.messenger.features.user.domain.Role;
import ru.connect.messenger.features.user.domain.User;
import ru.connect.messenger.features.user.dto.UserCreateRequest;

import java.time.OffsetDateTime;

@AllArgsConstructor
@Slf4j
@Configuration
@Profile("dev")
public class DataInitializer {
    private Faker faker;
    private UserService userService;
    private MessageService messageService;
    private ChatService chatService;

    @Bean
    public CommandLineRunner initializeDatabase() {
        return (args) -> {
            // Если юзер репозиторий пустой создаем админа
            if (userService.getTotalUsers() == 0) {
                createUserAdmin(
                        "dimarik70",
                        "Дмитрий",
                        "Горбунов",
                        "1234"
                );
            } else if (!userService.findUsersByRole(Role.ROLE_ADMIN).isEmpty()) {
                User admin = userService.findUsersByRole(Role.ROLE_ADMIN).getFirst();
                log.info("Admin user is already exist: {} ", admin.getUsername());
            }
            createTestUsers(20);
            createTestChat(60);
        };
    }

    public void createUserAdmin(
            String userName,
            String firstName,
            String lastName,
            String password
    ) {
        var adminData = new UserCreateRequest(
                userName,
                firstName,
                lastName,
                password,
                "token"
        );
        var admin = userService.create(adminData, Role.ROLE_ADMIN);
        log.info("✅ Admin user created successfully!");
        log.info("📧 UserName: {}", admin.getUsername());
    }

    public void createTestUsers(Integer count) {
        for (int i = 0; i < count; i++) {
            var userData = new UserCreateRequest(
                    faker.name().firstName().toLowerCase(),
                    faker.name().firstName(),
                    faker.name().lastName(),
                    faker.lorem().characters(8),
                    "token"
            );
            var newUser = userService.create(userData, Role.ROLE_USER);
            log.info("✅ New user created, username: {}", newUser.getUsername());
            log.info(" Password: {}", userData.password());
        }
    }

    public void createTestChat(int messageCount) {
        var userData1 = new UserCreateRequest(
                "vova91",
                "Вова",
                "Хроныч",
                "1234",
                "token"
        );

        var userData2 = new UserCreateRequest(
                "dron70",
                "Дрон",
                "Дроныч",
                "1234",
                "token"
        );
        var user1 = userService.create(userData1, Role.ROLE_USER);
        log.info("✅ New chat user created, username: {}", user1.getUsername());
        var user2 = userService.create(userData2, Role.ROLE_USER);
        log.info("✅ New chat user created, username: {}", user2.getUsername());
        var chat = chatService.createOrGetDirectChat(user1.getId(), user2.getId());
        log.info("✅ Chat created id: {}", chat.getId());

        // user to user
        for (int i = 0; i < messageCount; i++) {
            var senderId = user1.getId().toString();
            var receiverId = user2.getId().toString();
            if (i % 2 == 0) {
                senderId = user2.getId().toString();
                receiverId = user1.getId().toString();
            }
            var message = new SendMessageRequest();
            message.setChatId(chat.getId().toString());
            message.setSenderId(senderId);
            message.setReceiverId(receiverId);
            message.setText(faker.word().conjunction());
            message.setTimestamp(OffsetDateTime.now().toString());
            messageService.createMessage(message);
            log.info("✅ Create new messaging");
        }
    }
}
