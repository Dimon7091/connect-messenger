package ru.gorbunov.connect.core.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gorbunov.connect.core.dto.user.UpdateUserNameRequest;
import ru.gorbunov.connect.core.dto.user.UserCreateRequest;
import ru.gorbunov.connect.core.dto.user.UserPrivateResponse;
import ru.gorbunov.connect.core.exception.ResourceNotFoundException;
import ru.gorbunov.connect.core.mapper.UserMapper;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.models.User;
import ru.gorbunov.connect.core.repository.UserRepository;
import ru.gorbunov.connect.core.specifications.UserSpecification;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
@Transactional
@Slf4j
public class UserService {
    @PersistenceContext
    private EntityManager entityManager;
    private UserRepository userRepository;
    private UserMapper mapper;

    public User create(UserCreateRequest requestData, Role role) {
        var user = mapper.toEntity(requestData);
        user.setRole(role);
        return userRepository.save(user);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

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

    public UserPrivateResponse findByUserName(String userName) {
        var user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        return mapper.toPrivateDto(user);
    }

    public List<User> findUsersByRole(Role role) {
        return userRepository.findAllByRoles(role);
    }

    public Long findUserIdByUserName(String userName) {
        return userRepository.findUserIdByUserName(userName)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    public boolean isAdmin(Long userId) {
        return userRepository.isAdmin(userId);
    }

    public boolean isAdmin(String userName) {
        return userRepository.isAdmin(userName);
    }

    public boolean isDeleted(Long userId) {
        return userRepository.isDeleted(userId);
    }

    public User updateUserName(Long id, UpdateUserNameRequest requestData) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        mapper.updateUserName(requestData, user);
        return userRepository.save(user);
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Пользователь не найден");
        }
        userRepository.deleteById(id);
    }
}
