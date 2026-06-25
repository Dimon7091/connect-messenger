package ru.gorbunov.connect.core.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.gorbunov.connect.core.models.UserStatus;
import ru.gorbunov.connect.core.repository.UserStatusRepository;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StatusService {

    private final UserStatusRepository userStatusRepository;
    private final Map<Long, UserStatus> cache = new ConcurrentHashMap<>();

    // Вызывается из WebSocket контроллера при каждом пинге/сообщении
    public void updateInCache(Long userId, UserStatus.Status status) {
        cache.put(userId, new UserStatus(userId, status, OffsetDateTime.now()));
    }

    public UserStatus getStatus(Long userId) {
        var cached = cache.get(userId);
        if (cached != null) {
            return cached;
        }
        return userStatusRepository.findById(userId).orElse(null);
    }

    public long getAllOnlineUsersCount() {
        long totalOnlineUsers = 0;
        for (var entry : cache.entrySet()) {
            if (entry.getValue().getStatus().equals(UserStatus.Status.ONLINE)) {
                totalOnlineUsers ++;
            }
        }
        return totalOnlineUsers;
    }

    public void deleteStatusFromDatabase(Long userId) {
        userStatusRepository.deleteById(userId);
    }

    @Scheduled(fixedRate = 60000) // Раз в минуту
    public void syncWithDb() {
        if (cache.isEmpty()) {
            return;
        }

        // Для 1 ядра лучше итерироваться и обновлять
        cache.forEach((id, statusObj) -> {
            userStatusRepository.save(statusObj);
        });

        // Очищаем кэш для тех, кто уже давно OFFLINE, чтобы не забить 1ГБ ОЗУ
        cache.entrySet().removeIf(entry ->
                UserStatus.Status.OFFLINE.equals(entry.getValue().getStatus())
                        && entry.getValue().getLastSeen().isBefore(OffsetDateTime.now().minusMinutes(5))
        );
    }
}

