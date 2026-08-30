package ru.connect.messenger.features.user.service;

import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.connect.messenger.core.SecurityUser;
import ru.connect.messenger.features.user.domain.User;
import ru.connect.messenger.features.user.repository.UserRepository;

@Service
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public SecurityUser loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Ищем чистую сущность из БД
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 2. Создаем техническую обёртку ядра и передаем туда данные
        return new SecurityUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRoles().toString()
        );
    }
}
