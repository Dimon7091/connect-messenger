package ru.gorbunov.connect.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.datafaker.Faker;

@Configuration
public class FakerConfig {
    @Bean
    public Faker faker() {
        return new Faker();
    }
}
