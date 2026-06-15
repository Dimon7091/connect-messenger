package ru.gorbunov.connect.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class UserStatusSubscriptionService {
    // Кто на кого подписан: subscriberId -> Set<targetUserId>
    private final Map<Long, Set<Long>> subscriptions = new ConcurrentHashMap<>();
    // Обратный индекс для быстрой рассылки: targetUserId -> Set<subscriberId>
    private final Map<Long, Set<Long>> subscribersByTarget = new ConcurrentHashMap<>();

    public void subscribe(Long subscriberId, Long targetUserId) {
        subscriptions.computeIfAbsent(subscriberId, k -> ConcurrentHashMap.newKeySet()).add(targetUserId);
        subscribersByTarget.computeIfAbsent(targetUserId, k -> ConcurrentHashMap.newKeySet()).add(subscriberId);
        log.debug("Пользователь {} подписался на статус пользователя {}", subscriberId, targetUserId);
    }

    public void unsubscribe(Long subscriberId, Long targetUserId) {
        if (subscriberId == null || targetUserId == null) {
            return;
        }
        Set<Long> targets = subscriptions.get(subscriberId);
        if (targets != null) {
            targets.remove(targetUserId);
            if (targets.isEmpty()) {
                subscriptions.remove(subscriberId);
            }
        }
        Set<Long> subscribers = subscribersByTarget.get(targetUserId);
        if (subscribers != null) {
            subscribers.remove(subscriberId);
            if (subscribers.isEmpty()) {
                subscribersByTarget.remove(targetUserId);
            }
        }
        log.debug("Пользователь {} отписался от статуса пользователя {}", subscriberId, targetUserId);
    }

    // Получить всех подписчиков на статус targetUserId
    public Set<Long> getSubscribers(Long targetUserId) {
        return subscribersByTarget.getOrDefault(targetUserId, Collections.emptySet());
    }

    // При отключении пользователя очищаем его подписки и удаляем его из чужих подписок
    public void cleanupUserFully(Long userId) {
        cleanupSubscribers(userId);
        cleanupTarget(userId);
        log.debug("Произведена полная очистка подписок для пользователя {}", userId);
    }

    public void cleanupSubscribers(Long userId) {
        // Удаляем подписки, которые сделал этот пользователь
        Set<Long> targets = subscriptions.remove(userId);
        if (targets != null) {
            for (Long target : targets) {
                Set<Long> subs = subscribersByTarget.get(target);
                if (subs != null) {
                    subs.remove(userId);
                    if (subs.isEmpty()) {
                        subscribersByTarget.remove(target);
                    }
                }
            }
        }
    }

    public void cleanupTarget(Long userId) {
        // Удаляем этого пользователя из подписок других (на случай, если на него кто-то подписан)
        Set<Long> subscribers = subscribersByTarget.remove(userId);
        if (subscribers != null) {
            for (Long sub : subscribers) {
                Set<Long> targetsOfSub = subscriptions.get(sub);
                if (targetsOfSub != null) {
                    targetsOfSub.remove(userId);
                    if (targetsOfSub.isEmpty()) {
                        subscriptions.remove(sub);
                    }
                }
            }
        }
    }
}
