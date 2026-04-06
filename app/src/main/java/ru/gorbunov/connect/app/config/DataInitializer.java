package ru.gorbunov.connect.app.config;

import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gorbunov.connect.core.dto.UserCreateRequest;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.models.User;
import ru.gorbunov.connect.core.repository.UserRepository;
import ru.gorbunov.connect.core.service.UserService;

@Slf4j
@Configuration
public class DataInitializer {

    @Autowired
    private Faker faker;

    @Autowired
    private UserService userService;

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
            createTestUsers(4);
        };
    }

    public void createUserAdmin(String email,
                                String userName,
                                String firstName,
                                String lastName,
                                String password) {

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
        }
    }
}