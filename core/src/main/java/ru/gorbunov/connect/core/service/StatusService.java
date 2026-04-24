package ru.gorbunov.connect.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.gorbunov.connect.core.models.UserStatus;
import ru.gorbunov.connect.core.repository.UserStatusRepository;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class StatusService {

    private final UserStatusRepository repository;
    private final Map<Long, UserStatus> cache = new ConcurrentHashMap<>();

    // Вызывается из WebSocket контроллера при каждом пинге/сообщении
    public void updateInCache(Long userId, String status) {
        cache.put(userId, new UserStatus(userId, status, OffsetDateTime.now()));
    }

    @Scheduled(fixedRate = 60000) // Раз в минуту
    public void syncWithDb() {
        if (cache.isEmpty()) return;

        // Для 1 ядра лучше итерироваться и обновлять
        cache.forEach((id, statusObj) -> {
            repository.upsertStatus(id, statusObj.getStatus(), statusObj.getLastSeen());
        });

        // Очищаем кэш для тех, кто уже давно OFFLINE, чтобы не забить 1ГБ ОЗУ
        cache.entrySet().removeIf(entry ->
                "OFFLINE".equals(entry.getValue().getStatus()) &&
                        entry.getValue().getLastSeen().isBefore(OffsetDateTime.now().minusMinutes(5))
        );
    }
}

