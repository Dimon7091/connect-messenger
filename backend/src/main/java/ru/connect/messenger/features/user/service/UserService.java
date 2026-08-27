package ru.connect.messenger.features.user.service;

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
import ru.connect.messenger.core.exception.ResourceNotFoundException;
import ru.connect.messenger.features.user.domain.Role;
import ru.connect.messenger.features.user.domain.User;
import ru.connect.messenger.features.user.dto.UpdateUserNameRequest;
import ru.connect.messenger.features.user.dto.UserCreateRequest;
import ru.connect.messenger.features.user.dto.UserPrivateResponse;
import ru.connect.messenger.features.user.mapper.UserMapper;
import ru.connect.messenger.features.user.repository.UserRepository;
import ru.connect.messenger.features.user.specifications.UserSpecification;

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

    public List<User> findUsersInBatches(List<Long> ids) {
        List<User> result = new ArrayList<>();
        int batchSize = 500; // Размер одного пакета

        for (int i = 0; i < ids.size(); i += batchSize) {
            // Вычисляем конечный индекс, чтобы не выйти за границы списка
            int end = Math.min(i + batchSize, ids.size());
            List<Long> batch = ids.subList(i, end);

            // Вызываем репозиторий для текущего батча
            result.addAll(userRepository.findAllById(batch));
        }

        return result;
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
        var isAdmin = userRepository.isAdmin(userId);
        if (isAdmin == null) {
            throw new ResourceNotFoundException("Пользователь не найден");
        }
        return isAdmin;
    }

    public boolean isAdmin(String userName) {
        var isAdmin = userRepository.isAdmin(userName);
        if (isAdmin == null) {
            throw new ResourceNotFoundException("Пользователь не найден");
        }
        return isAdmin;
    }

    public boolean isDeleted(Long userId) {
        var isDeleted =  userRepository.isDeleted(userId);
        if (isDeleted == null) {
            throw new ResourceNotFoundException("Пользователь не найден");
        }
        return isDeleted;
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
