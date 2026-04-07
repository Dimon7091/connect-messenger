package ru.gorbunov.connect.core.service;

import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import ru.gorbunov.connect.core.dto.UserCreateRequest;
import ru.gorbunov.connect.core.dto.UserPatchUpdateRequest;
import ru.gorbunov.connect.core.dto.UserPutUpdateRequest;
import ru.gorbunov.connect.core.dto.UserResponse;
import ru.gorbunov.connect.core.dto.UserStatResponse;
import ru.gorbunov.connect.core.exception.EmailAlreadyExistsException;
import ru.gorbunov.connect.core.exception.ResourceNotFoundException;
import ru.gorbunov.connect.core.exception.UserNameAlreadyExistsException;
import ru.gorbunov.connect.core.mapper.UserMapper;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.models.User;
import ru.gorbunov.connect.core.repository.UserRepository;


import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper mapper;

    // --- Create ---
    public UserResponse create(UserCreateRequest requestData, Role role) {
        if (userRepository.existsByEmail(requestData.email())) {
            throw new EmailAlreadyExistsException(
                    "Пользователь с email: " + requestData.email() + " уже существует"
            );
        }

        if (userRepository.existsByUserName(requestData.userName())) {
            throw new UserNameAlreadyExistsException(
                    "Пользователь с именем пользователя: " + requestData.userName() + " уже существует"
            );
        }

        var user = mapper.toEntity(requestData);
        user.setRole(role);
        var savedUser = userRepository.save(user);
        return mapper.toDto(savedUser);
    }

    // --- Read ---
    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(mapper::toDto);
    }

    public UserResponse findById(Long id) {
        var user = userRepository.findById(id).
                orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        return mapper.toDto(user);
    }

    public UserStatResponse getUsersStat() {
        Long totalUsers = userRepository.count();
        Long onlineUsers = 0L;
        return new UserStatResponse(totalUsers, onlineUsers);
    }

    public Long totalUsers() {
        return userRepository.count();
    }

    public UserDetails findByUserName(String userName) {
        return userRepository.findByUserName(userName)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    public List<User> findUsersByRole(Role role) {
        return userRepository.findAllByRoles(role);
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
