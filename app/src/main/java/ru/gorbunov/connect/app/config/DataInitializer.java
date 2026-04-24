package ru.gorbunov.connect.app.config;

import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gorbunov.connect.core.dto.chat.ChatCreateOrGetRequest;
import ru.gorbunov.connect.core.dto.user.UserCreateRequest;
import ru.gorbunov.connect.core.dto.ws.SendMessageRequest;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.models.User;
import ru.gorbunov.connect.core.repository.UserRepository;
import ru.gorbunov.connect.core.service.ChatService;
import ru.gorbunov.connect.core.service.MessageService;
import ru.gorbunov.connect.core.service.UserService;

import java.time.OffsetDateTime;

@Slf4j
@Configuration
public class DataInitializer {

    @Autowired
    private Faker faker;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserRepository userRepository;

    @Bean
    public CommandLineRunner initializeDatabase() {
        return (args) -> {
            // Если юзер репозиторий пустой создаем админа
            if (userService.totalUsers() == 0) {
                createUserAdmin(
                        "dimarik70rus@gmail.com",
                        "dimarik70",
                        "Дмитрий",
                        "Горбунов",
                        "1234"
                );
            } else if (!userService.findUsersByRole(Role.ROLE_ADMIN).isEmpty()) {
                User admin = userService.findUsersByRole(Role.ROLE_ADMIN).getFirst();
                log.info("Admin user is already exist: {} ", admin.getEmail());
            }
            createTestUsers(20);
            createTestChat();
        };
    }

    public void createUserAdmin(
            String email,
            String userName,
            String firstName,
            String lastName,
            String password
    ) {
        var adminData = new UserCreateRequest(
                email,
                userName,
                firstName,
                lastName,
                password
        );
        var admin = userService.create(adminData, Role.ROLE_ADMIN);
        log.info("✅ Admin user created successfully!");
        log.info("📧 UserName: {}", admin.userName());
    }

    public void createTestUsers(Integer count) {
        for (int i = 0; i < count; i++) {
            var userData = new UserCreateRequest(
                    faker.internet().emailAddress(),
                    faker.name().firstName().toLowerCase(),
                    faker.name().firstName(),
                    faker.name().lastName(),
                    faker.lorem().characters(8)
            );
            var newUser = userService.create(userData, Role.ROLE_USER);
            log.info("✅ New user created, username: {}", newUser.userName());
            log.info(" Password: {}", userData.password());
        }
    }

    public void createTestChat() {
        var userData1 = new UserCreateRequest(
                "dafsasddfd@gmail.com",
                "vova91",
                "Вова",
                "Хроныч",
                "1234"
        );

        var userData2 = new UserCreateRequest(
                "dafsdfd@gmail.com",
                "dron70",
                "Дрон",
                "Дроныч",
                "1234"
        );
        var user1 = userService.create(userData1, Role.ROLE_USER);
        log.info("✅ New chat user created, username: {}", user1.userName());
        var user2 = userService.create(userData2, Role.ROLE_USER);
        log.info("✅ New chat user created, username: {}", user2.userName());

        var chat = chatService.createOrGetDirectChat(user1.id(), user2.id());
        log.info("✅ Chat created id: {}", chat.getId());

        // user1 to user2
        for (int i = 0; i < 5; i++) {
            var message = new SendMessageRequest();
            message.setChatId(chat.getId().toString());
            message.setSenderId(user1.id().toString());
            message.setReceiverId(user2.id().toString());
            message.setText(faker.word().toString());
            message.setTimestamp(OffsetDateTime.now().toString());
            messageService.createMessage(message);
            log.info("✅ Create new message");
        }
        // user2 to user1
        for (int i = 0; i < 5; i++) {
            var message = new SendMessageRequest();
            message.setChatId(chat.getId().toString());
            message.setSenderId(user2.id().toString());
            message.setReceiverId(user1.id().toString());
            message.setText(faker.word().toString());
            message.setTimestamp(OffsetDateTime.now().toString());
            messageService.createMessage(message);
            log.info("✅ Create new message");
        }
    }
}