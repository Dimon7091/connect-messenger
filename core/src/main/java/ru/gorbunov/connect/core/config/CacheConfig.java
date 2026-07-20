package ru.gorbunov.connect.core.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gorbunov.connect.core.models.BlockId;

@Configuration
public class CacheConfig {
    @Bean
    public Cache<Long, Boolean> banCache() {
        return Caffeine.newBuilder()
                .maximumSize(1500)
                .build();
    }

    @Bean
    public Cache<Long, Boolean> usersDeletedCache() {
        return Caffeine.newBuilder()
                .maximumSize(1500)
                .build();
    }

    @Bean
    public Cache<BlockId, Boolean> userBlockCache() {
        return Caffeine.newBuilder()
                .maximumSize(4000)
                .build();
    }
}
