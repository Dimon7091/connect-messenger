package ru.gorbunov.connect.core.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gorbunov.connect.core.dto.user.UpdateUserNameRequest;
import ru.gorbunov.connect.core.dto.user.UserCreateRequest;
import ru.gorbunov.connect.core.dto.user.UserResponse;
import ru.gorbunov.connect.core.exception.EmailAlreadyExistsException;
import ru.gorbunov.connect.core.exception.ResourceNotFoundException;
import ru.gorbunov.connect.core.exception.UserNameAlreadyExistsException;
import ru.gorbunov.connect.core.mapper.UserMapper;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.models.User;
import ru.gorbunov.connect.core.repository.UserRepository;
import ru.gorbunov.connect.core.specifications.UserSpecification;

import java.util.List;

@Service
@Transactional
@Slf4j
public class UserService {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper mapper;

    // --- Create ---
    public UserResponse create(UserCreateRequest requestData, Role role) {
        if (userRepository.existsByUserName(requestData.userName())) {
            throw new UserNameAlreadyExistsException(
                    "Пользователь с именем пользователя: " + requestData.userName() + " уже существует"
            );
        }

        var user = mapper.toEntity(requestData);
        user.setRole(role);
        var createdUser = userRepository.save(user);
        return mapper.toDto(createdUser);
    }

    // --- Read ---
    public User getUserById(Long id) {
        return userRepository.findById(id).
                orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    public Page<User> findAllUsersWithPagination(
            Integer page,
            Integer size,
            String userName,
            String sortBy,
            String sortDir
    ) {
        Specification<User> spec = UserSpecification.hasUserName(userName);
        Sort sort = Sort.unsorted();
        if (sortBy != null && sortDir != null) {
            try {
                Sort.Direction direction = Sort.Direction.fromString(sortDir);
                sort = Sort.by(direction, sortBy);
            } catch (IllegalArgumentException e) {
                // если направление не asc/desc – можно проигнорировать или вернуть ошибку
                sort = Sort.unsorted();
            }
        }
        Pageable pageable = PageRequest.of(page, size, sort);
        return userRepository.findAll(spec, pageable);
    }

    public List<User> findByUserNameStartingWith(String userName) {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("deletedUserFilter");
        return userRepository.findByUserNameStartingWith(userName);
    }

    public String getUserFullName(Long userId) {
        return userRepository.findFullNameById(userId)
                .orElse(null);
    }

    public long getTotalUsers() {
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
        return userRepository.save(user);
    }

    // --- Delete ---
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Пользователь не найден");
        }
        userRepository.deleteById(id);
    }
}
