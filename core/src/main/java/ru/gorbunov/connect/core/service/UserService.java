package ru.gorbunov.connect.core.service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.gorbunov.connect.core.dto.user.UpdateUserNameRequest;
import ru.gorbunov.connect.core.dto.user.UserCreateRequest;
import ru.gorbunov.connect.core.dto.user.UserResponse;
import ru.gorbunov.connect.core.dto.user.UserStatResponse;
import ru.gorbunov.connect.core.exception.EmailAlreadyExistsException;
import ru.gorbunov.connect.core.exception.ResourceNotFoundException;
import ru.gorbunov.connect.core.exception.UserNameAlreadyExistsException;
import ru.gorbunov.connect.core.mapper.UserMapper;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.models.User;
import ru.gorbunov.connect.core.repository.UserRepository;


import java.util.List;

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
    public User getUserById(Long id) {
        return userRepository.findById(id).
                orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public List<User> findByUserNameStartingWith(String userName) {
        return userRepository.findByUserNameStartingWith(userName);
    }

    public String getUserFullName(Long userId) {
        return userRepository.findFullNameById(userId)
                .orElse(null);
    }

    public UserStatResponse getUsersStat() {
        Long totalUsers = userRepository.count();
        Long onlineUsers = 0L;
        return new UserStatResponse(totalUsers, onlineUsers);
    }

    public Long totalUsers() {
        return userRepository.count();
    }

    public UserResponse findByUserName(String userName) {
        var user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        return mapper.toDto(user);
    }

    public List<User> findUsersByRole(Role role) {
        return userRepository.findAllByRoles(role);
    }

    public Long findUserIdByUserName(String userName) {
        return userRepository.findUserIdByUserName(userName)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    // --- Update ---
    public User updateUserName(Long id, UpdateUserNameRequest requestData) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        mapper.updateUserName(requestData, user);
        userRepository.save(user);
        return user;
    }

    // --- Delete ---
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Пользователь не найден");
        }
        userRepository.deleteById(id);
    }
}
