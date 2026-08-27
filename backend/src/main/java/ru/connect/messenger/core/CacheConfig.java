package ru.connect.messenger.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.connect.messenger.features.user.domain.BlockId;

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
