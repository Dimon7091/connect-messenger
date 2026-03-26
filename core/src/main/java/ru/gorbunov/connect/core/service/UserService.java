package ru.gorbunov.connect.core.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gorbunov.connect.core.dto.UserCreateRequest;
import ru.gorbunov.connect.core.dto.UserPatchUpdateRequest;
import ru.gorbunov.connect.core.dto.UserPutUpdateRequest;
import ru.gorbunov.connect.core.dto.UserResponse;
import ru.gorbunov.connect.core.exception.EmailAlreadyExistsException;
import ru.gorbunov.connect.core.exception.ResourceNotFoundException;
import ru.gorbunov.connect.core.exception.UserNameAlreadyExistsException;
import ru.gorbunov.connect.core.mapper.UserMapper;
import ru.gorbunov.connect.core.repository.UserRepository;

import java.util.List;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper mapper;

    // --- Create ---
    public UserResponse create(UserCreateRequest requestData) {
        if (!userRepository.existsByEmail(requestData.email())) {
            throw new EmailAlreadyExistsException(
                    "Пользователь с email: " + requestData.email() + " уже существует"
            );
        }

        if (!userRepository.existsByUserName(requestData.userName())) {
            throw new UserNameAlreadyExistsException(
                    "Пользователь с именем пользователя: " + requestData.userName() + " уже существует"
            );
        }

        var user = mapper.toEntity(requestData);
        var savedUser = userRepository.save(user);
        return mapper.toDto(savedUser);
    }

    // --- Read ---
    public List<UserResponse> findAll() {
        var users = userRepository.findAll();
        return users.stream().map(user -> mapper.toDto(user)).toList();
    }

    public UserResponse findById(Long id) {
        var user = userRepository.findById(id).
                orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        return mapper.toDto(user);
    }

    // --- Update ---
    public UserResponse putUpdate(Long id, UserPutUpdateRequest requestData) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        if (!user.getEmail().equals(requestData.email())
                && userRepository.existsByEmail(requestData.email())) {
            throw new EmailAlreadyExistsException("email " + requestData.email() + " уже занят");
        }

        mapper.putUpdate(requestData, user);
        userRepository.save(user);
        return mapper.toDto(user);
    }

    public UserResponse patchUpdate(Long id, UserPatchUpdateRequest requestData) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        // Обрабатываем email (если он передан в запросе)
        if (requestData.email() != null && requestData.email().isPresent()) {
            String newEmail = requestData.email().get();

            if (!user.getEmail().equals(newEmail)
                    && userRepository.existsByEmail(newEmail)) {
                throw new EmailAlreadyExistsException(
                        "email " + newEmail + " уже занят"
                );
            }
        }
        mapper.patchUpdate(requestData, user);
        return mapper.toDto(user);
    }

    // --- Delete ---
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Пользователь не найден");
        }
        userRepository.deleteById(id);
    }
}
