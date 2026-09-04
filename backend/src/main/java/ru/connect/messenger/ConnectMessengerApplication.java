package ru.connect.messenger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "ru.connect.messenger")
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
public class ConnectMessengerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConnectMessengerApplication.class);
    }
}
