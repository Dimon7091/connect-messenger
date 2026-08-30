package ru.connect.messenger.features.user.api;

public interface UserBlockChecker {
    boolean isEitherBlocked(Long userA, Long userB);
}
